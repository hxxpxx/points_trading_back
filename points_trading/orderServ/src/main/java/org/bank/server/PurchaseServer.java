//package org.bank.server;
//
//import org.apache.shiro.authz.annotation.RequiresPermissions;
//import org.bank.aop.annotation.LogAnnotation;
//import org.bank.constants.Constant;
//import org.bank.entity.Purchase;
//import org.bank.entity.PurchaseSnapshot;
//import org.bank.service.PurchaseService;
//import org.bank.utils.DataResult;
//import org.bank.utils.JwtTokenUtil;
//import org.bank.vo.req.PurchaseAddReqVO;
//import org.bank.vo.req.PurchasePageReqVO;
//import org.bank.vo.resp.PageVO;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.validation.Valid;
//
//@RestController("purchaseServer")
//public class PurchaseServer {
//    @Autowired
//    PurchaseService purchaseService;
//
//    @PostMapping("/purchase/add")
//    @LogAnnotation(title = "订单管理", action = "订单新增")
//    public DataResult<Purchase> addpurchase(@RequestBody @Valid PurchaseAddReqVO vo, HttpServletRequest request) {
//        DataResult<Purchase> result = DataResult.success();
//        String userId = JwtTokenUtil.getUserId(request.getHeader(Constant.ACCESS_TOKEN));
//        result.setData(purchaseService.addPurchase(vo,userId));
//        return result;
//    }
//    @PostMapping("/purchase/page")
//    @LogAnnotation(title = "订单管理", action = "分页获取订单信息")
//    public DataResult<PageVO<Purchase>> pageInfo(@RequestBody PurchasePageReqVO vo,HttpServletRequest request) {
//
//        DataResult<PageVO<Purchase>> result = DataResult.success();
//        String userId = JwtTokenUtil.getUserId(request.getHeader(Constant.ACCESS_TOKEN));
//        vo.setUserId(userId);
//        result.setData(purchaseService.pageInfo(vo));
//        return result;
//    }
//
//    @GetMapping("/purchase/getPurchaseSnapshotForPurchaseNo/{purchaseNo}")
//    @LogAnnotation(title = "订单管理", action = "获取订单交易快照")
//    public DataResult<PurchaseSnapshot> getPurchaseSnapshotForPurchaseNo(@PathVariable("purchaseNo")String purchaseNo) {
//        DataResult<PurchaseSnapshot> result = DataResult.success();
//        result.setData(purchaseService.getPurchaseSnapshotForPurchaseNo(purchaseNo));
//        return result;
//    }
//
//    @PostMapping("/purchase/getPurchaseNo")
//    @LogAnnotation(title = "订单管理", action = "获取订单号")
//    public DataResult<String> getPurchaseNo() {
//        DataResult<String> result = DataResult.success();
//        result.setData(purchaseService.issuePurchaseNo());
//        return result;
//    }
//
//    @GetMapping("/purchase/cancelPurchaseNo/{purchaseNo}")
//    @LogAnnotation(title = "订单管理", action = "释放订单号")
//    public DataResult<Boolean> cancelPurchaseNo(@PathVariable("purchaseNo")String purchaseNo ) {
//        DataResult<Boolean> result = DataResult.success();
//        result.setData(purchaseService.cancelPurchaseNo(purchaseNo));
//        return result;
//    }
//
//    @GetMapping("/purchase/cancelPurchase/{purchaseNo}")
//    @LogAnnotation(title = "订单管理", action = "取消订单")
//    public DataResult cancelPurchase(@PathVariable("purchaseNo")String purchaseNo,HttpServletRequest request) {
//        DataResult result= DataResult.success();
//        String userId = JwtTokenUtil.getUserId(request.getHeader(Constant.ACCESS_TOKEN));
//        purchaseService.canclePurchase(purchaseNo,userId);
//        return result;
//    }
//
//    @GetMapping("/purchase/payPurchase/{purchaseNo}")
//    @LogAnnotation(title = "订单管理", action = "支付订单")
//    public DataResult payPurchase(@PathVariable("purchaseNo")String purchaseNo,HttpServletRequest request) {
//        DataResult result = DataResult.success();
//        String userId = JwtTokenUtil.getUserId(request.getHeader(Constant.ACCESS_TOKEN));
//        purchaseService.payPurchase(purchaseNo,userId);
//        return result;
//    }
//
//}