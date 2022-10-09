package org.bank.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.bank.client.FinancialClient;
import org.bank.client.UserClient;
import org.bank.constants.Constant;
import org.bank.entity.Financial;
import org.bank.entity.Purchase;
import org.bank.entity.PurchaseSnapshot;
import org.bank.entity.UserPurchase;
import org.bank.exception.BusinessException;
import org.bank.exception.code.BaseResponseCode;
import org.bank.lock.redisson.RedissonLock;
import org.bank.mapper.PurchaseMapper;
import org.bank.mapper.UserPurchaseMapper;
import org.bank.message.PurchaseDelayMessageVO;
import org.bank.message.PurchasePayMessageVO;
import org.bank.service.MsgSenderService;
import org.bank.service.PurchaseService;
import org.bank.service.PurchaseSnapshotService;
import org.bank.service.RedisService;
import org.bank.utils.DataResult;
import org.bank.utils.PageUtils;
import org.bank.vo.req.*;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {
    @Autowired
    PurchaseMapper purchaseMapper;

    @Autowired
    UserPurchaseMapper userPurchaseMapper;

    @Autowired
    UserClient userClient;

    @Autowired
    RedisService redisService;

    @Autowired
    RedissonLock redissonLock;

    @Autowired
    FinancialClient financialClient;

    @Autowired
    PurchaseSnapshotService purchaseSnapshotService;

    @Autowired
    MsgSenderService msgSenderService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private long redisTimeOut=1000*60*60;

    /**
     * 签发订单号给页面
     *
     * @return
     */
    @Override
    public String issuePurchaseNo() {
        //获取分布式锁
        try {
            if (redissonLock.lock("getPurchaseNoLock", 10)) {
                String purchaseNo = "purchase_" + UUID.randomUUID().toString();
                return purchaseNo;
            } else {
                throw new BusinessException(BaseResponseCode.GET_ORDERNO_AWAIT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            //释放锁
            redissonLock.release("getPurchaseNoLock");
        }
    }

    @Override
    public boolean cancelPurchaseNo(String purchaseNo) {
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    /**
     * 创建订单
     * @param vo
     * @return
     */
    @Override
    public Purchase addPurchase(PurchaseAddReqVO vo, String userId) {
        String purchaseNo = vo.getPurchaseNo();

        if(purchaseNo.startsWith("sell")){
            Purchase addSell = new Purchase();
            BeanUtils.copyProperties(vo, addSell);

            UserPurchase userPurchase = new UserPurchase();
            userPurchase.setUserId(addSell.getUserId());
            userPurchase.setFinancialId(addSell.getFinancialId());

            if(addSell.getNum()  >  userPurchaseMapper.selectAll(userPurchase).get(0).getHoldNum()){
                log.info("订单创建失败，失败原因为{}", "持有份额不足");
                throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
            }
            //组装数据
            addSell = makerderData(addSell);
            addSell.setUserId(userId);
            addSell.setPrice(financialClient.detailInfo(addSell.getFinancialId()).getData().getPrice());
            //数据校验
            checkPurchaseData(addSell);
            addSell.setStatus("unsell");
            int insertSize = 0;
            insertSize = purchaseMapper.insert(addSell);
            //交易快照
            purchaseSnapshotService.addPurchaseSnapshot(addSell.getPurchaseSnapshot());
            if (insertSize <= 0) {
                throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
            }
            //调用积分增加
            UserUpdateIntegralReqVO userUpdateIntegralReqVO=new UserUpdateIntegralReqVO();
            userUpdateIntegralReqVO.setId(addSell.getUserId());
            userUpdateIntegralReqVO.setIntegral(addSell.getTotalAmout());
            userUpdateIntegralReqVO.setType(false);
            return addSell;
        }

        //获取锁，消费订单号
        Purchase addPurchase=null;
        boolean isUnLockStock=false;
        //获取分布式锁
        try {
            if (redissonLock.lock(Constant.ORDERNO_LOCK_KEY + purchaseNo, 10)) {
                //如果没有商品ID  直接返回
                if(org.apache.commons.lang3.StringUtils.isBlank(vo.getFinancialId())){
                    throw new BusinessException(BaseResponseCode.GET_ORDER_GOODS_ERROR);
                }
                if(vo.getNum()==null||vo.getNum()<=0){
                    throw new BusinessException(BaseResponseCode.GET_ORDER_NUM_ERROR);
                }
                addPurchase = new Purchase();
                BeanUtils.copyProperties(vo, addPurchase);
                //组装数据
                addPurchase = makerderData(addPurchase);
                addPurchase.setUserId(userId);
                //数据校验
                checkPurchaseData(addPurchase);
                int insertSize = 0;
                insertSize = purchaseMapper.insert(addPurchase);
                if (insertSize <= 0) {
                    throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
                }
                //交易快照
                purchaseSnapshotService.addPurchaseSnapshot(addPurchase.getPurchaseSnapshot());
               //锁定库存
                FinancialStockReqVO financialStockReqVO = new FinancialStockReqVO();
                financialStockReqVO.setFinancialId(addPurchase.getFinancialId());
                financialStockReqVO.setStackNum(addPurchase.getNum());
                DataResult<Boolean> financialStockLockResult = financialClient.lockStock(financialStockReqVO);
                if (financialStockLockResult.getCode()!=0) {
                    log.info("订单创建失败，失败原因为{}", financialStockLockResult.getMsg());
                    throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
                }else{
                    isUnLockStock=true;
                }
                //发送延时消息
                //参数一：topic   如果想添加tag,可以使用"topic:tag"的写法
                //参数二：Message<?>
                //参数三：消息发送超时时间
                //参数四：delayLevel 延时level  messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                PurchaseDelayMessageVO purchaseDelayMessageVO=new PurchaseDelayMessageVO();
                purchaseDelayMessageVO.setPurchaseNo(vo.getPurchaseNo());
                SendResult sentResult= rocketMQTemplate.syncSend(String.format("%s:%s", Constant.PURCHASE_MQ_TOPIC_KEY, Constant.PURCHASE_DELAY_PAY_TAG_KEY), MessageBuilder.withPayload(JSONObject.toJSONString(purchaseDelayMessageVO)).build(),3000,16);
                return addPurchase;
            }else{
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }catch (Exception e){
            log.error("创建订单异常，异常原因为{}",e.getMessage());
            if(isUnLockStock){
                FinancialStockReqVO financialStockReqVO=new FinancialStockReqVO();
                financialStockReqVO.setStackNum(addPurchase.getNum());
                financialStockReqVO.setFinancialId(addPurchase.getFinancialId());
                financialClient.unLockStock(financialStockReqVO);
//              msgSenderService.sendMessage(Constant.ORDER_MQ_TOPIC_KEY,Constant.ORDER_CANCEL_TAG_KEY, JSONObject.toJSONString(financialStockReqVO));
            }
            if(e instanceof  BusinessException){
                throw e;
            }else{
                throw new BusinessException(BaseResponseCode.GET_ORDER_CREATE_ERROR);
            }

        }finally {
            redissonLock.release(Constant.ORDERNO_LOCK_KEY + purchaseNo);
        }

    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    @Override
    public void canclePurchase(String purchaseNo) {
        //校验订单号
        if(StringUtils.isNotBlank(purchaseNo)){
            Purchase purchaseInfo= detailInfo(purchaseNo);
            //只有未支付的单子可以取消订单操作
            if(purchaseInfo!=null&&StringUtils.isNotBlank(purchaseInfo.getFinancialId())&&"unpaid".equals(purchaseInfo.getStatus())){
                //取消订单
                Purchase update=new Purchase();
                update.setPurchaseNo(purchaseNo);
                update.setStatus("cancel");
                update.setUpdatetime(new Date());
                int updateSize=purchaseMapper.updateByPrimaryKeySelective(update);

                if(updateSize>0){
                    //解锁商品库存
                    FinancialStockReqVO financialStockReqVO=new FinancialStockReqVO();
                    financialStockReqVO.setStackNum(purchaseInfo.getNum());
                    financialStockReqVO.setFinancialId(purchaseInfo.getFinancialId());
                    msgSenderService.sendMessage(Constant.PURCHASE_MQ_TOPIC_KEY,Constant.PURCHASE_CANCEL_TAG_KEY, JSONObject.toJSONString(financialStockReqVO));
                }else{
                    throw new BusinessException(BaseResponseCode.CANCLE_ORDER_ERROR);
                }
            }else{
                throw new BusinessException(BaseResponseCode.CANCLE_ORDER_STATUS_ERROR);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    @Override
    public void canclePurchase(String purchaseNo,String userId) {
        //校验订单号
        if(StringUtils.isNotBlank(purchaseNo)){
            Purchase purchaseInfo= detailInfo(purchaseNo);
            if(StringUtils.isBlank(userId)||!userId.equals(purchaseInfo.getUserId())){
                throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
            }
            //只有未支付的单子可以取消订单操作
            if(purchaseInfo!=null&&StringUtils.isNotBlank(purchaseInfo.getFinancialId())
                    &&("unpaid".equals(purchaseInfo.getStatus())||"unsell".equals(purchaseInfo.getStatus()))){
                //取消订单
                Purchase update=new Purchase();
                update.setPurchaseNo(purchaseNo);
                update.setStatus("cancel");
                update.setUpdatetime(new Date());
                int updateSize=purchaseMapper.updateByPrimaryKeySelective(update);

                if(updateSize>0){
                    //解锁商品库存
                    FinancialStockReqVO financialStockReqVO=new FinancialStockReqVO();
                    financialStockReqVO.setStackNum(purchaseInfo.getNum());
                    financialStockReqVO.setFinancialId(purchaseInfo.getFinancialId());
                    msgSenderService.sendMessage(Constant.ORDER_MQ_TOPIC_KEY,Constant.ORDER_CANCEL_TAG_KEY, JSONObject.toJSONString(financialStockReqVO));
                }else{
                    throw new BusinessException(BaseResponseCode.CANCLE_ORDER_ERROR);
                }
            }else{
                throw new BusinessException(BaseResponseCode.CANCLE_ORDER_STATUS_ERROR);
            }
        }
    }

    @Override
    public void payPurchase(String purchaseNo,String userId) {
        //校验订单号
        try{
            if(StringUtils.isNotBlank(purchaseNo)) {
                Purchase purchaseInfo = detailInfo(purchaseNo);
                if (purchaseInfo == null || StringUtils.isBlank(purchaseInfo.getFinancialId())) {
                    throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ORDERNO_ERRPR);
                }
                if(purchaseInfo.getUserId()==null||!purchaseInfo.getUserId().equals(userId)){
                    //非本人结束操作
                    throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
                }
                //判断订单状态
                if(!"unpaid".equals(purchaseInfo.getStatus()) && !"unsell".equals(purchaseInfo.getStatus())){
                    throw new BusinessException(BaseResponseCode.PAY_ORDER_STATUS_ERRPR);
                }
                //订单支付
                //1  修改订单状态为支付中
                Purchase update = new Purchase();
                update.setPurchaseNo(purchaseNo);
                update.setStatus("paying");
                update.setUpdatetime(new Date());
                int updateSize = purchaseMapper.updateByPrimaryKeySelective(update);
                if (updateSize > 0) {
                    //2发送事务消息  扣除积分与库存
                    PurchasePayMessageVO purchasePayMessageVO=new PurchasePayMessageVO();
                    BeanUtils.copyProperties(purchaseInfo,purchasePayMessageVO);
                    TransactionSendResult transactionSendResult = rocketMQTemplate.sendMessageInTransaction
                            (String.format("%s:%s", Constant.PURCHASE_MQ_TOPIC_KEY, Constant.PURCHASE_PAY_TAG_KEY), MessageBuilder.withPayload(purchasePayMessageVO).build(), purchasePayMessageVO);
                }else{
                    throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ERRPR);
                }
            }else{
                throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ORDERNO_ERRPR);
            }
        }catch (Exception e){
            throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ERRPR);
        }

    }

    @Override
    public void updatePurchase(PurchaseUpdateReqVO vo) {
        String purchaseNo=vo.getPurchaseNo();
        if(StringUtils.isNotBlank(purchaseNo)){
            Purchase purchaseInfo=detailInfo(purchaseNo);
            if(purchaseInfo==null||StringUtils.isBlank(purchaseInfo.getFinancialId())){
                return;
            }
            Purchase update=new Purchase();
            BeanUtils.copyProperties(vo,update);
            update.setUpdatetime(new Date());
            int updateSize=purchaseMapper.updateByPrimaryKeySelective(update);
            if(updateSize==0){
                throw  new BusinessException(BaseResponseCode.OPERATION_ERRO);
            }
        }
    }

    /**
     * @param purchase
     * @return
     */
    private boolean checkPurchaseData(Purchase purchase) {
        //校验订单号
        Purchase checkPurchase = detailInfo(purchase.getPurchaseNo());
        if (checkPurchase != null) {
            throw new BusinessException(BaseResponseCode.GET_ORDERNO_ISEXIT);
        }
        //校验商品
        if (purchase.getFinancial() == null || StringUtils.isEmpty(purchase.getTitle())) {
            throw new BusinessException(BaseResponseCode.GET_ORDER_GOODS_ERROR);
        }
        return true;
    }

    //组装数据
    private Purchase makerderData(Purchase purchase) {
        Date createTime = new Date();
        purchase.setCreatetime(createTime);
        purchase.setStatus("unpaid");

        //商品信息
        String financialId = purchase.getFinancialId();
        if (!StringUtils.isEmpty(financialId)) {
            DataResult<Financial> financialDataResult = financialClient.detailInfo(financialId);
            if (financialDataResult.getCode()==0 && financialDataResult.getData() != null) {
                Financial financial = financialDataResult.getData();
                purchase.setFinancial(financial);
                purchase.setTitle(financial.getName());
                Double price = financial.getPrice() == null ? 0.00 : financial.getPrice();
                Double totalAmout = null;
                BigDecimal priceDc = new BigDecimal(price.toString());
                BigDecimal numDc = new BigDecimal((purchase.getNum() == null ? new Integer(0) : purchase.getNum()).toString());
                totalAmout = priceDc.multiply(numDc).doubleValue();
                purchase.setTotalAmout(totalAmout);
                //交易快照信息
                PurchaseSnapshot purchaseSnapshot = new PurchaseSnapshot();
                purchaseSnapshot.setPurchaseNo(purchase.getPurchaseNo());
                purchaseSnapshot.setCreatetime(createTime);
                purchaseSnapshot.setFinancialId(financialId);
                purchaseSnapshot.setTitle(financial.getName());
                purchaseSnapshot.setDescribe(financial.getDescribe());
                purchaseSnapshot.setPrice(financial.getPrice());
                purchase.setPurchaseSnapshot(purchaseSnapshot);
            }
        }
        return purchase;
    }


    @Override
    public Purchase detailInfo(String purchaseNo,String userId) {
        Purchase purchase = purchaseMapper.selectByPrimaryKey(purchaseNo);
        if(StringUtils.isBlank(userId)||!userId.equals(purchase.getUserId())){
            throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
        }
        return purchase;
    }
    @Override
    public Purchase detailInfo(String purchaseNo) {
        Purchase purchase = purchaseMapper.selectByPrimaryKey(purchaseNo);
        return purchase;
    }



    @Override
    public PageVO<Purchase> pageInfo(PurchasePageReqVO vo) {
        Purchase query=new Purchase();
        BeanUtils.copyProperties(vo,query);
        PageHelper.startPage(vo.getPageNum(), vo.getPageSize());
        BeanUtils.copyProperties(vo, query);
        List<Purchase> purchaseList = purchaseMapper.selectAll(query);
        return PageUtils.getPageVO(purchaseList);
    }

    @Override
    public PurchaseSnapshot getPurchaseSnapshotForPurchaseNo(String purchaseNo) {
        return purchaseSnapshotService.detailInfo(purchaseNo);
    }

}
