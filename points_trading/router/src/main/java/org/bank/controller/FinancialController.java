package org.bank.controller;

import org.bank.aop.annotation.LogAnnotation;
import org.bank.client.FinancialClient;
import org.bank.entity.Financial;
import org.bank.utils.DataResult;
import org.bank.vo.req.FinancialAddReqVO;
import org.bank.vo.req.FinancialPageReqVO;
import org.bank.vo.req.FinancialStockReqVO;
import org.bank.vo.req.FinancialUpdateReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RequestMapping("/financial")
@RestController
public class FinancialController {

    @Autowired
    FinancialClient financialClient;

    @PostMapping("/add")
    public DataResult<Financial> addFinancial(@RequestBody @Valid FinancialAddReqVO vo) {
        return financialClient.addFinancial(vo);
    }

    @DeleteMapping("/delete/{id}")
    public DataResult deleted(@PathVariable("id") String id) {
        return financialClient.deleted(id);
    }

    @PutMapping("/update")
    public DataResult update(@RequestBody @Valid FinancialUpdateReqVO vo) {
        return financialClient.update(vo);
    }

    @GetMapping("/detail/{id}")
    public DataResult<Financial> detailInfo(@PathVariable("id") String id) {
        return financialClient.detailInfo(id);
    }

    @PostMapping("/getStock")
    @LogAnnotation(title = "商品管理", action = "获取商品库存")
    public DataResult<Integer> getStock(@RequestBody FinancialStockReqVO vo){
        FinancialStockReqVO req=new FinancialStockReqVO();
        req.setFinancialId("financial_56811ac9-f866-4cf1-9083-605dc384c89d");
        DataResult<Integer> stockResult=financialClient.getStock(req);
        return stockResult;
    }

    @PostMapping("/page")
    public DataResult<PageVO<Financial>> pageInfo(@RequestBody FinancialPageReqVO vo) {
        return financialClient.pageInfo(vo);
    }
}
