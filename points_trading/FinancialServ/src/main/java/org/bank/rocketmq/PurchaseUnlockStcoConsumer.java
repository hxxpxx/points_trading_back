package org.bank.rocketmq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.bank.constants.Constant;
import org.bank.service.FinancialService;
import org.bank.vo.req.FinancialStockReqVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @BelongsProject: points_trading
 * @BelongsPackage: org.bank.rocketmq
 * @Author: lizongle
 * @CreateTime: 2022-07-22  10:44
 * @Description: 预解锁库存消息
 * @Version: 1.0
 */
@Service
@RocketMQMessageListener(consumerGroup = Constant.FINANCIAL_SERVER_MQ_GROUP+Constant.PURCHASE_CANCEL_TAG_KEY,topic = Constant.PURCHASE_MQ_TOPIC_KEY,selectorExpression = Constant.PURCHASE_CANCEL_TAG_KEY)
@Slf4j
public class PurchaseUnlockStcoConsumer implements RocketMQListener<String> {
    @Autowired
    FinancialService financialService;
    @Override
    public void onMessage(String s) {
        log.info("准备消费预解锁金融产品库存消息{}",s);
        try{
            FinancialStockReqVO financialStockReqVO= JSONObject.parseObject(s,FinancialStockReqVO.class);
            financialService.unLockStock(financialStockReqVO.getStackNum(),financialStockReqVO.getFinancialId());
        }catch (Exception e){
            log.info("消费预解锁金融产品库存消息失败:{}",e.getMessage());
            throw e;
        }
    }
}
