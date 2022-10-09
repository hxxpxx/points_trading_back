package org.bank.client;

import org.bank.aop.annotation.LogAnnotation;
import org.bank.entity.Purchase;
import org.bank.entity.PurchaseSnapshot;
import org.bank.utils.DataResult;
import org.bank.vo.req.PurchaseAddReqVO;
import org.bank.vo.req.PurchasePageReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

@FeignClient(name = "purchaseService",contextId = "purchaseServer")
public interface PurchaseClient {
    @PostMapping("/purchase/add")
    public DataResult<Purchase> addpurchase(@RequestBody @Valid PurchaseAddReqVO vo) ;

    @PostMapping("/purchase/page")
    public DataResult<PageVO<Purchase>> pageInfo(@RequestBody PurchasePageReqVO vo);

    @PostMapping("/purchase/getSellNo")
    public DataResult<String> getSellNo();

    @PostMapping("/purchase/getPurchaseNo")
    public DataResult<String> getPurchaseNo();

    @GetMapping("/purchase/cancelPurchaseNo/{purchaseNo}")
    public DataResult<Boolean> cancelPurchaseNo(@PathVariable("purchaseNo")String purchaseNo) ;

    @GetMapping("/purchase/cancelPurchase/{purchaseNo}")
    @LogAnnotation(title = "订单管理", action = "取消订单")
    public DataResult cancelPurchase(@PathVariable("purchaseNo")String purchaseNo);

    @GetMapping("/purchase/payPurchase/{purchaseNo}")
    @LogAnnotation(title = "订单管理", action = "支付订单")
    public DataResult payPurchase(@PathVariable("purchaseNo")String purchaseNo) ;

    @GetMapping("/purchase/getPurchaseSnapshotForPurchaseNo/{purchaseNo}")
    @LogAnnotation(title = "订单管理", action = "获取订单交易快照")
    public DataResult<PurchaseSnapshot> getPurchaseSnapshotForPurchaseNo(@PathVariable("purchaseNo")String purchaseNo) ;
}
