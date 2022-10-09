package org.bank.rocketmq;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.bank.constants.Constant;
import org.bank.message.PurchasePayMessageVO;
import org.bank.service.FinancialService;
import org.bank.vo.req.FinancialDeductStockReqVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @BelongsProject: points_trading
 * @BelongsPackage: org.bank.rockermq
 * @Author: lizongle
 * @CreateTime: 2022-07-21  16:44
 * @Description:
 * @Version: 1.0
 */
@Service
@RocketMQMessageListener(consumerGroup = Constant.FINANCIAL_SERVER_MQ_GROUP+Constant.PURCHASE_PAY_TAG_KEY,topic = Constant.PURCHASE_MQ_TOPIC_KEY,selectorExpression = Constant.PURCHASE_PAY_TAG_KEY)
@Slf4j
public class PurchaseTransactionConsumer implements RocketMQListener<String> {

    @Autowired
    FinancialService financialService;

    @Override
    public void onMessage(String s) {
        log.info("准备扣除金融产品库存消息{}",s);
        try{
            PurchasePayMessageVO purchasePayMessageVO= JSONObject.parseObject(s,PurchasePayMessageVO.class);
            FinancialDeductStockReqVO reqvo=new FinancialDeductStockReqVO();
            reqvo.setId(purchasePayMessageVO.getFinancialId());
            reqvo.setStock(purchasePayMessageVO.getNum());
            financialService.deductStock(reqvo);
        }catch (Exception e){
            log.info("准备扣除金融产品库存消息失败:{}",e.getMessage());
            throw e;
        }
    }
}
