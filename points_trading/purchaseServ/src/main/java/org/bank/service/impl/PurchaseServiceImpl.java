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
     * ????????????????????????
     *
     * @return
     */
    @Override
    public String issuePurchaseNo() {
        //??????????????????
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
            //?????????
            redissonLock.release("getPurchaseNoLock");
        }
    }

    @Override
    public boolean cancelPurchaseNo(String purchaseNo) {
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    /**
     * ????????????
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
                log.info("????????????????????????????????????{}", "??????????????????");
                throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
            }
            //????????????
            addSell = makerderData(addSell);
            addSell.setUserId(userId);
            addSell.setPrice(financialClient.detailInfo(addSell.getFinancialId()).getData().getPrice());
            //????????????
            checkPurchaseData(addSell);
            addSell.setStatus("unsell");
            int insertSize = 0;
            insertSize = purchaseMapper.insert(addSell);
            //????????????
            purchaseSnapshotService.addPurchaseSnapshot(addSell.getPurchaseSnapshot());
            if (insertSize <= 0) {
                throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
            }
            //??????????????????
            UserUpdateIntegralReqVO userUpdateIntegralReqVO=new UserUpdateIntegralReqVO();
            userUpdateIntegralReqVO.setId(addSell.getUserId());
            userUpdateIntegralReqVO.setIntegral(addSell.getTotalAmout());
            userUpdateIntegralReqVO.setType(false);
            return addSell;
        }

        //???????????????????????????
        Purchase addPurchase=null;
        boolean isUnLockStock=false;
        //??????????????????
        try {
            if (redissonLock.lock(Constant.ORDERNO_LOCK_KEY + purchaseNo, 10)) {
                //??????????????????ID  ????????????
                if(org.apache.commons.lang3.StringUtils.isBlank(vo.getFinancialId())){
                    throw new BusinessException(BaseResponseCode.GET_ORDER_GOODS_ERROR);
                }
                if(vo.getNum()==null||vo.getNum()<=0){
                    throw new BusinessException(BaseResponseCode.GET_ORDER_NUM_ERROR);
                }
                addPurchase = new Purchase();
                BeanUtils.copyProperties(vo, addPurchase);
                //????????????
                addPurchase = makerderData(addPurchase);
                addPurchase.setUserId(userId);
                //????????????
                checkPurchaseData(addPurchase);
                int insertSize = 0;
                insertSize = purchaseMapper.insert(addPurchase);
                if (insertSize <= 0) {
                    throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
                }
                //????????????
                purchaseSnapshotService.addPurchaseSnapshot(addPurchase.getPurchaseSnapshot());
               //????????????
                FinancialStockReqVO financialStockReqVO = new FinancialStockReqVO();
                financialStockReqVO.setFinancialId(addPurchase.getFinancialId());
                financialStockReqVO.setStackNum(addPurchase.getNum());
                DataResult<Boolean> financialStockLockResult = financialClient.lockStock(financialStockReqVO);
                if (financialStockLockResult.getCode()!=0) {
                    log.info("????????????????????????????????????{}", financialStockLockResult.getMsg());
                    throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
                }else{
                    isUnLockStock=true;
                }
                //??????????????????
                //????????????topic   ???????????????tag,????????????"topic:tag"?????????
                //????????????Message<?>
                //????????????????????????????????????
                //????????????delayLevel ??????level  messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                PurchaseDelayMessageVO purchaseDelayMessageVO=new PurchaseDelayMessageVO();
                purchaseDelayMessageVO.setPurchaseNo(vo.getPurchaseNo());
                SendResult sentResult= rocketMQTemplate.syncSend(String.format("%s:%s", Constant.PURCHASE_MQ_TOPIC_KEY, Constant.PURCHASE_DELAY_PAY_TAG_KEY), MessageBuilder.withPayload(JSONObject.toJSONString(purchaseDelayMessageVO)).build(),3000,16);
                return addPurchase;
            }else{
                throw new BusinessException(BaseResponseCode.SYSTEM_BUSY);
            }
        }catch (Exception e){
            log.error("????????????????????????????????????{}",e.getMessage());
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
        //???????????????
        if(StringUtils.isNotBlank(purchaseNo)){
            Purchase purchaseInfo= detailInfo(purchaseNo);
            //????????????????????????????????????????????????
            if(purchaseInfo!=null&&StringUtils.isNotBlank(purchaseInfo.getFinancialId())&&"unpaid".equals(purchaseInfo.getStatus())){
                //????????????
                Purchase update=new Purchase();
                update.setPurchaseNo(purchaseNo);
                update.setStatus("cancel");
                update.setUpdatetime(new Date());
                int updateSize=purchaseMapper.updateByPrimaryKeySelective(update);

                if(updateSize>0){
                    //??????????????????
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
        //???????????????
        if(StringUtils.isNotBlank(purchaseNo)){
            Purchase purchaseInfo= detailInfo(purchaseNo);
            if(StringUtils.isBlank(userId)||!userId.equals(purchaseInfo.getUserId())){
                throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
            }
            //????????????????????????????????????????????????
            if(purchaseInfo!=null&&StringUtils.isNotBlank(purchaseInfo.getFinancialId())
                    &&("unpaid".equals(purchaseInfo.getStatus())||"unsell".equals(purchaseInfo.getStatus()))){
                //????????????
                Purchase update=new Purchase();
                update.setPurchaseNo(purchaseNo);
                update.setStatus("cancel");
                update.setUpdatetime(new Date());
                int updateSize=purchaseMapper.updateByPrimaryKeySelective(update);

                if(updateSize>0){
                    //??????????????????
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
        //???????????????
        try{
            if(StringUtils.isNotBlank(purchaseNo)) {
                Purchase purchaseInfo = detailInfo(purchaseNo);
                if (purchaseInfo == null || StringUtils.isBlank(purchaseInfo.getFinancialId())) {
                    throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ORDERNO_ERRPR);
                }
                if(purchaseInfo.getUserId()==null||!purchaseInfo.getUserId().equals(userId)){
                    //?????????????????????
                    throw new BusinessException(BaseResponseCode.OP_ORDER_USER_ERROR);
                }
                //??????????????????
                if(!"unpaid".equals(purchaseInfo.getStatus()) && !"unsell".equals(purchaseInfo.getStatus())){
                    throw new BusinessException(BaseResponseCode.PAY_ORDER_STATUS_ERRPR);
                }
                //????????????
                //1  ??????????????????????????????
                Purchase update = new Purchase();
                update.setPurchaseNo(purchaseNo);
                update.setStatus("paying");
                update.setUpdatetime(new Date());
                int updateSize = purchaseMapper.updateByPrimaryKeySelective(update);
                if (updateSize > 0) {
                    //2??????????????????  ?????????????????????
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
        //???????????????
        Purchase checkPurchase = detailInfo(purchase.getPurchaseNo());
        if (checkPurchase != null) {
            throw new BusinessException(BaseResponseCode.GET_ORDERNO_ISEXIT);
        }
        //????????????
        if (purchase.getFinancial() == null || StringUtils.isEmpty(purchase.getTitle())) {
            throw new BusinessException(BaseResponseCode.GET_ORDER_GOODS_ERROR);
        }
        return true;
    }

    //????????????
    private Purchase makerderData(Purchase purchase) {
        Date createTime = new Date();
        purchase.setCreatetime(createTime);
        purchase.setStatus("unpaid");

        //????????????
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
                //??????????????????
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
