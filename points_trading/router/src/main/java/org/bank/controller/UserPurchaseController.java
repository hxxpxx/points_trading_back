package org.bank.controller;


import org.bank.client.UserPurchaseClient;
import org.bank.entity.UserPurchase;
import org.bank.utils.DataResult;
import org.bank.vo.req.UserPurchaseAddReqVO;
import org.bank.vo.req.UserPurchasePageReqVO;
import org.bank.vo.req.UserPurchaseUpdateReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RequestMapping("/userPurchase")
@RestController
public class UserPurchaseController {

    @Autowired
    UserPurchaseClient userPurchaseClient;

    @PostMapping("/add")
    public DataResult<UserPurchase> addUserPurchase(@RequestBody @Valid UserPurchaseAddReqVO vo) {
        return userPurchaseClient.addUserPurchase(vo);
    }

    @DeleteMapping("/delete/{id}")
    public DataResult deleted(@PathVariable("id") String id) {
        return userPurchaseClient.deleted(id);
    }

    @PutMapping("/update")
    public DataResult update(@RequestBody @Valid UserPurchaseUpdateReqVO vo) {
        return userPurchaseClient.update(vo);
    }

    @GetMapping("/detail/{id}")
    public DataResult<UserPurchase> detailInfo(@PathVariable("id") String id) {
        return userPurchaseClient.detailInfo(id);
    }

//    @PostMapping("/getStock")
//    @LogAnnotation(title = "商品管理", action = "获取商品库存")
//    public DataResult<Integer> getStock(@RequestBody UserPurchaseStockReqVO vo){
//        UserPurchaseStockReqVO req=new UserPurchaseStockReqVO();
//        req.setUserPurchaseId("userPurchase_56811ac9-f866-4cf1-9083-605dc384c89d");
//        DataResult<Integer> stockResult=userPurchaseClient.getStock(req);
//        return stockResult;
//    }

    @PostMapping("/page")
    public DataResult<PageVO<UserPurchase>> pageInfo(@RequestBody UserPurchasePageReqVO vo) {
        return userPurchaseClient.pageInfo(vo);
    }

}
