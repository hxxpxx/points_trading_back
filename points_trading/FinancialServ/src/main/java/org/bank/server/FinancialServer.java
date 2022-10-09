package org.bank.server;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bank.aop.annotation.LogAnnotation;
import org.bank.entity.Financial;
import org.bank.service.FinancialService;
import org.bank.utils.DataResult;
import org.bank.vo.req.*;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController("financialServer")
public class FinancialServer {

    @Autowired
    FinancialService financialService;

    @PostMapping("/financial/add")
    @LogAnnotation(title = "商品管理", action = "商品新增")
    @RequiresPermissions("financial:financial:add")
    public DataResult<Financial> addFinancial(@RequestBody @Valid FinancialAddReqVO vo) {
        DataResult<Financial> result = DataResult.success();
        result.setData(financialService.addFinancial(vo));
        return result;
    }

    @DeleteMapping("/financial/delete/{id}")
    @LogAnnotation(title = "商品管理", action = "删除商品")
    @RequiresPermissions("financial:financial:deleted")
    public DataResult deleted(@PathVariable("id") String id) {
        financialService.deletedFinancial(id);
        return DataResult.success();
    }

    @PutMapping("/financial/update")
    @LogAnnotation(title = "商品管理", action = "更新商品信息")
    @RequiresPermissions("financial:financial:update")
    public DataResult updateDept(@RequestBody @Valid FinancialUpdateReqVO vo) {
        financialService.updateFinancial(vo);
        return DataResult.success();
    }

    @GetMapping("/financial/detail/{id}")
    @LogAnnotation(title = "商品管理", action = "查询商品详情")
    @RequiresPermissions("financial:financial:list")
    public DataResult<Financial> detailInfo(@PathVariable("id") String id) {
        DataResult<Financial> result = DataResult.success();
        result.setData(financialService.detailInfo(id));
        return result;
    }

    @PostMapping("/financial/page")
    @LogAnnotation(title = "商品管理", action = "分页获取商品信息")
    @RequiresPermissions("financial:financial:list")
    public DataResult<PageVO<Financial>> pageInfo(@RequestBody FinancialPageReqVO vo) {
        DataResult<PageVO<Financial>> result = DataResult.success();
        result.setData(financialService.pageInfo(vo));
        return result;
    }


    @PostMapping("/financial/getStock")
    @LogAnnotation(title = "商品管理", action = "获取商品库存")
    public DataResult<Integer> getStock(@RequestBody FinancialStockReqVO vo) {
        DataResult<Integer> result = DataResult.success();
        result.setData(financialService.getStock(vo.getFinancialId()));
        return result;
    }

    @PostMapping("/financial/stock/lock")
    @LogAnnotation(title = "商品管理", action = "锁定商品库存")
    public DataResult<Boolean> lockStock(@RequestBody FinancialStockReqVO vo) {
        DataResult<Boolean> result = DataResult.success();
        result.setData(financialService.lockStock(vo.getStackNum(),vo.getFinancialId()));
        return result;
    }

    @PostMapping("/financial/stock/unlock")
    @LogAnnotation(title = "商品管理", action = "解锁商品库存")
    public DataResult<Boolean> unLockStock(@RequestBody FinancialStockReqVO vo) {
        DataResult<Boolean> result = DataResult.success();
        result.setData(financialService.unLockStock(vo.getStackNum(),vo.getFinancialId()));
        return result;
    }

    @PostMapping("/financial/stock/deduct")
    @LogAnnotation(title = "商品管理", action = "扣除商品库存")
    public DataResult deductStock(@RequestBody FinancialDeductStockReqVO vo) {
        DataResult result = DataResult.success();
        financialService.deductStock(vo);
        return result;
    }

}
