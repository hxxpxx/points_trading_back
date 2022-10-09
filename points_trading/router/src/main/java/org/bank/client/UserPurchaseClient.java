package org.bank.client;

import org.bank.entity.UserPurchase;
import org.bank.utils.DataResult;
import org.bank.vo.req.UserPurchaseAddReqVO;
import org.bank.vo.req.UserPurchasePageReqVO;
import org.bank.vo.req.UserPurchaseUpdateReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@FeignClient(name = "purchaseService", contextId = "userPurchaseServer")
public interface UserPurchaseClient {
    @PostMapping("/userPurchase/add")
    public DataResult<UserPurchase> addUserPurchase(@RequestBody @Valid UserPurchaseAddReqVO vo);

    @DeleteMapping("/userPurchase/delete/{id}")
    public DataResult deleted(@PathVariable("id") String id) ;

    @PutMapping("/userPurchase/update")
    public DataResult update(@RequestBody @Valid UserPurchaseUpdateReqVO vo);

//    @PostMapping("/userPurchase/getholdNum")
//    @LogAnnotation(title = "持有份额", action = "获取持有份额")
//    public DataResult<Integer> getStock(@RequestBody UserPurchaseHoldNumReqVO vo) ;

    @GetMapping("/userPurchase/detail/{id}")
    public DataResult<UserPurchase> detailInfo(@PathVariable("id") String id) ;

    @PostMapping("/userPurchase/page")
    public DataResult<PageVO<UserPurchase>> pageInfo(@RequestBody UserPurchasePageReqVO vo);


}
