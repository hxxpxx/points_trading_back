package org.bank.rockermq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.bank.client.UserClient;
import org.bank.constants.Constant;
import org.bank.entity.Purchase;
import org.bank.entity.UserPurchase;
import org.bank.exception.BusinessException;
import org.bank.exception.code.BaseResponseCode;
import org.bank.mapper.UserPurchaseMapper;
import org.bank.message.PurchaseDelayMessageVO;
import org.bank.message.PurchasePayMessageVO;
import org.bank.service.MsgSenderService;
import org.bank.service.PurchaseService;
import org.bank.utils.DataResult;
import org.bank.vo.req.PurchaseUpdateReqVO;
import org.bank.vo.req.UserUpdateIntegralReqVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @BelongsProject: points_trading
 * @BelongsPackage: org.bank.rockermq
 * @Author: lizongle
 * @CreateTime: 2022-07-21  15:29
 * @Description:
 * @Version: 1.0
 */
@Service
@RocketMQTransactionListener
@Slf4j
public class PurchaseTransactionListenerImpl implements RocketMQLocalTransactionListener {
    @Autowired
    UserClient userClient;

    @Autowired
    PurchaseService purchaseService;

    @Autowired
    UserPurchaseMapper userPurchaseMapper;

    @Autowired
    MsgSenderService msgSenderService;
    /**
     * @description:执行本地事务
     * @author: lizongle
     * @param: [message, o]
     * @return: org.apache.rocketmq.spring.core.RocketMQLocalTransactionState
     **/
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        log.info("准备消费订单支付事务消息");
        RocketMQLocalTransactionState resultState = RocketMQLocalTransactionState.COMMIT;
        boolean isUpdateIntegral=false;
        Purchase purchase = null;
        PurchasePayMessageVO vo=(PurchasePayMessageVO) o;
        if(o==null){
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        try {
            log.info("执行订单事务{}",vo.toString());
            //查询订单
            purchase=purchaseService.detailInfo(vo.getPurchaseNo());
            if(purchase==null|| StringUtils.isBlank(purchase.getUserId())){
                log.info("订单支付获取订单信息错误{}","订单不存在");
                throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ORDERNO_ERRPR);
            }
            //调用积分扣除
            UserUpdateIntegralReqVO userUpdateIntegralReqVO=new UserUpdateIntegralReqVO();
            userUpdateIntegralReqVO.setId(purchase.getUserId());
            userUpdateIntegralReqVO.setIntegral(purchase.getTotalAmout());

            //区分购买卖出
            if(purchase.getPurchaseNo().startsWith("sell")){
                userUpdateIntegralReqVO.setType(true);
            }
            else{
                userUpdateIntegralReqVO.setType(false);
            }

            DataResult dataResult=userClient.updateIntegral(userUpdateIntegralReqVO);
            if(dataResult.getCode()==0){
                isUpdateIntegral=true;
                Purchase update=new Purchase();
                update.setPaytime(new Date());
                if(purchase.getPurchaseNo().startsWith("sell")){
                    update.setStatus("selled");
                }
                else{
                    update.setStatus("paid");
                }
                update.setUpdatetime(new Date());
                update.setPurchaseNo(purchase.getPurchaseNo());
                PurchaseUpdateReqVO updateReqVO=new PurchaseUpdateReqVO();
                BeanUtils.copyProperties(update,updateReqVO);
                purchaseService.updatePurchase(updateReqVO);

                //将已支付的理财添加用户持有的理财产品表中
                UserPurchase userPurchase = new UserPurchase();
                userPurchase.setUserId(purchase.getUserId());
                userPurchase.setFinancialId(purchase.getFinancialId());

                List<UserPurchase> userPurchaseList = userPurchaseMapper.selectAll(userPurchase);
                if(userPurchaseList.isEmpty()){
                    String id = "up_" + UUID.randomUUID().toString();
                    userPurchase.setId(id);
                    userPurchase.setHoldNum(purchase.getNum().doubleValue());
                    userPurchase.setTitle(purchase.getTitle());
                    userPurchase.setCreatetime(new Date());
                    userPurchaseMapper.insert(userPurchase);
                }
                else{
                    //购买和卖出区分
                    double holdNum = 0;
                    if(purchase.getPurchaseNo().startsWith("sell")){
                        holdNum = userPurchaseList.get(0).getHoldNum() - purchase.getNum().doubleValue();
                    }
                    else{
                        holdNum = purchase.getNum().doubleValue() + userPurchaseList.get(0).getHoldNum();
                    }
                    userPurchase.setHoldNum(holdNum);
                    userPurchase.setUpdatetime(new Date());
                    userPurchase.setTitle(purchase.getTitle());
                    userPurchase.setId(userPurchaseList.get(0).getId());
                    userPurchaseMapper.updateByPrimaryKey(userPurchase);
                }

            }else{
                log.info("订单【"+purchase.getPurchaseNo()+"】扣除积分信息失败，原因为{}",dataResult.getMsg());
                throw new BusinessException(BaseResponseCode.PAY_ORDERNO_ERRPR);
            }
        }catch (Exception e){
            log.info("准备消费订单支付事务消息异常，异常原因为{}",e.getMessage());
            if(isUpdateIntegral){
                //调用退款接口
                UserUpdateIntegralReqVO userUpdateIntegralReqVO=new UserUpdateIntegralReqVO();
                userUpdateIntegralReqVO.setId(purchase.getUserId());
                userUpdateIntegralReqVO.setIntegral(purchase.getTotalAmout());
                userUpdateIntegralReqVO.setType(true);
                msgSenderService.sendMessage(Constant.ORDER_MQ_TOPIC_KEY,Constant.ORDER_PAY_ADD_TAG_KEY,JSONObject.toJSONString(userUpdateIntegralReqVO));
            }
            if(purchase!=null){
                Purchase update=new Purchase();
                PurchaseUpdateReqVO updateReqVO=new PurchaseUpdateReqVO();
                updateReqVO.setStatus("fail");
                updateReqVO.setPurchaseNo(purchase.getPurchaseNo());
                update.setUpdatetime(new Date());
                purchaseService.updatePurchase(updateReqVO);
                //预解锁商品库存
                PurchaseDelayMessageVO purchaseDelayMessageVO=new PurchaseDelayMessageVO();
                purchaseDelayMessageVO.setPurchaseNo(purchase.getPurchaseNo());
                msgSenderService.sendMessage(Constant.PURCHASE_MQ_TOPIC_KEY,Constant.PURCHASE_DELAY_PAY_TAG_KEY, JSONObject.toJSONString(purchaseDelayMessageVO));
            }
            resultState=RocketMQLocalTransactionState.ROLLBACK;
        }
        return resultState;
    }

    /**
     * @description:检查本地事务
     * @author: lizongle
     * @param: [message]
     * @return: org.apache.rocketmq.spring.core.RocketMQLocalTransactionState
     **/
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        log.info("checkLocalTransaction");
        RocketMQLocalTransactionState resultState = RocketMQLocalTransactionState.UNKNOWN;
        try {
            PurchasePayMessageVO vo= JSONObject.parseObject(message.toString(),PurchasePayMessageVO.class);
            log.info("检查订单事务{}",vo.toString());
            //查询订单
            Purchase purchase=purchaseService.detailInfo(vo.getPurchaseNo());
            if(purchase==null|| StringUtils.isBlank(purchase.getUserId())){
                log.info("订单支付获取订单信息错误{}","订单不存在");
                resultState=RocketMQLocalTransactionState.ROLLBACK;
                return resultState;
            }
            if("paid".equals(purchase.getStatus())){
                resultState=RocketMQLocalTransactionState.COMMIT;
            }
            if("fail".equals(purchase.getStatus())||"cancel".equals(purchase.getStatus())){
                resultState=RocketMQLocalTransactionState.ROLLBACK;
            }
        }catch (Exception e){
            resultState=RocketMQLocalTransactionState.ROLLBACK;
        }
        return resultState;
    }
}
