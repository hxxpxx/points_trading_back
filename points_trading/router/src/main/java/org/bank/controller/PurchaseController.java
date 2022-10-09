package org.bank.controller;

import org.bank.aop.annotation.LogAnnotation;
import org.bank.client.PurchaseClient;
import org.bank.entity.Purchase;
import org.bank.entity.PurchaseSnapshot;
import org.bank.utils.DataResult;
import org.bank.vo.req.PurchaseAddReqVO;
import org.bank.vo.req.PurchasePageReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RequestMapping("/purchase")
@RestController
public class PurchaseController {

    @Autowired
    PurchaseClient purchaseClient;

    @PostMapping("/add")
    public DataResult<Purchase> addpurchase(@RequestBody @Valid PurchaseAddReqVO vo){
        return purchaseClient.addpurchase(vo);
    }

    @PostMapping("/page")
    public DataResult<PageVO<Purchase>> pageInfo(@RequestBody PurchasePageReqVO vo){
        return purchaseClient.pageInfo(vo);
    }

    @PostMapping("/getSellNo")
    public DataResult<String> getSellNo(){
        return purchaseClient.getSellNo();
    }

    @PostMapping("/getPurchaseNo")
    public DataResult<String> getPurchaseNo(){
        return purchaseClient.getPurchaseNo();
    }

    @GetMapping("/cancelPurchaseNo/{purchaseNo}")
    public DataResult<Boolean> cancelPurchaseNo(@PathVariable("purchaseNo")String purchaseNo){
        return purchaseClient.cancelPurchaseNo(purchaseNo);
    }
    @GetMapping("/cancelPurchase/{purchaseNo}")
    @LogAnnotation(title = "订单管理", action = "取消订单")
    public DataResult cancelPurchase(@PathVariable("purchaseNo")String purchaseNo){
        return purchaseClient.cancelPurchase(purchaseNo);
    }

    @GetMapping("/payPurchase/{purchaseNo}")
    @LogAnnotation(title = "订单管理", action = "支付订单")
    public DataResult payPurchase(@PathVariable("purchaseNo")String purchaseNo) {
        return purchaseClient.payPurchase(purchaseNo);
    }

    @GetMapping("/getPurchaseSnapshotForPurchaseNo/{purchaseNo}")
    @LogAnnotation(title = "订单管理", action = "获取订单交易快照")
    public DataResult<PurchaseSnapshot> getPurchaseSnapshotForPurchaseNo(@PathVariable("purchaseNo")String purchaseNo) {
        return purchaseClient.getPurchaseSnapshotForPurchaseNo(purchaseNo);
    }
}
