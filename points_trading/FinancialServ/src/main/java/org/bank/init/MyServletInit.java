package org.bank.init;

import org.bank.constants.Constant;
import org.bank.entity.Financial;
import org.bank.service.FinancialService;
import org.bank.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;


@Component
public class MyServletInit {
    @Autowired
    FinancialService financialService;

    @Autowired
    RedisService redisService;
    @PostConstruct
    public void init(){
        //将商品添加到redis中去
        initFinancial();
    }

    public void initFinancial(){
        List<Financial> financialList=financialService.selectAllFinancial();
        if(financialList!=null&&financialList.size()>0){
            for(Financial financial:financialList){
                redisService.set(Constant.FINANCIAL_CACHE_KEY+financial.getId(),financial);
            }
        }
    }
}
