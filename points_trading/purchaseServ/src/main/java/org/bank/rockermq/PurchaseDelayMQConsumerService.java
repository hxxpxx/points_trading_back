package org.bank.rockermq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.bank.constants.Constant;
import org.bank.exception.BusinessException;
import org.bank.exception.code.BaseResponseCode;
import org.bank.message.PurchaseDelayMessageVO;
import org.bank.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @BelongsProject: points_trading
 * @BelongsPackage: org.bank.service
 * @Author: lizongle
 * @CreateTime: 2022-07-20  16:40
 * @Description: 订单超时未支付消息消费
 * @Version: 1.0
 */
@Service
@RocketMQMessageListener(consumerGroup = Constant.PURCHASE_SERVER_MQ_GROUP,topic = Constant.PURCHASE_MQ_TOPIC_KEY,selectorExpression = Constant.PURCHASE_DELAY_PAY_TAG_KEY)
@Slf4j
public class PurchaseDelayMQConsumerService implements RocketMQListener<String> {
    @Autowired
    PurchaseService purchaseService;

    @Override
    public void onMessage(String s) {
        log.info("开始消费延时消息{}",s);
        try{
            PurchaseDelayMessageVO purchaseDelayMessageVO= JSONObject.parseObject(s,PurchaseDelayMessageVO.class);
            purchaseService.canclePurchase(purchaseDelayMessageVO.getPurchaseNo());
        }catch (Exception e){
            log.info("订单超时未支付取消订单失败:{}",e.getMessage());
            if(e instanceof BusinessException){
                if(BaseResponseCode.CANCLE_ORDER_STATUS_ERROR.getCode()!=((BusinessException) e).getMessageCode()){
                    throw e;
                }else{
                    log.info("订单超时未支付取消订单失败:{}",e.getMessage());
                }
            }else{
                throw e;
            }
        }
    }
}
