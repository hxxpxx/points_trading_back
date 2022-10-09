package org.bank.client;

import org.bank.aop.annotation.LogAnnotation;
import org.bank.entity.Financial;
import org.bank.utils.DataResult;
import org.bank.vo.req.*;
import org.bank.vo.resp.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@FeignClient(name = "financialService", contextId = "financialServer")
public interface FinancialClient {
    @PostMapping("/financial/add")
    public DataResult<Financial> addFinancial(@RequestBody @Valid FinancialAddReqVO vo);

    @DeleteMapping("/financial/delete/{id}")
    public DataResult deleted(@PathVariable("id") String id) ;

    @PutMapping("/financial/update")
    public DataResult update(@RequestBody @Valid FinancialUpdateReqVO vo);

    @GetMapping("/financial/detail/{id}")
    public DataResult<Financial> detailInfo(@PathVariable("id") String id) ;

    @PostMapping("/financial/page")
    public DataResult<PageVO<Financial>> pageInfo(@RequestBody FinancialPageReqVO vo);

    @PostMapping("/financial/getStock")
    @LogAnnotation(title = "金融产品管理", action = "获取金融产品库存")
    public DataResult<Integer> getStock(@RequestBody FinancialStockReqVO vo) ;

    @PostMapping("/financial/stock/lock")
    public DataResult<Boolean> lockStock(@RequestBody FinancialStockReqVO vo);

    @PostMapping("/financial/stock/unlock")
    public DataResult<Boolean> unLockStock(@RequestBody FinancialStockReqVO vo);

    @PostMapping("/financial/stock/deduct")
    @LogAnnotation(title = "金融产品管理", action = "扣除金融产品库存")
    public DataResult deductStock(@RequestBody FinancialDeductStockReqVO vo);
}
