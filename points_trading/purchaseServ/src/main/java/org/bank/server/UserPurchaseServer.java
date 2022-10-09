package org.bank.server;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bank.aop.annotation.LogAnnotation;
import org.bank.entity.UserPurchase;
import org.bank.service.UserPurchaseService;
import org.bank.utils.DataResult;
import org.bank.vo.req.UserPurchasePageReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("userPurchaseServer")
public class UserPurchaseServer {

    @Autowired
    UserPurchaseService userPurchaseService;

    @PostMapping("/userPurchase/page")
    @LogAnnotation(title = "理财持有份额管理", action = "分页获取理财持有份额信息")
    @RequiresPermissions("userPurchase:userPurchase:list")
    public DataResult<PageVO<UserPurchase>> pageInfo(@RequestBody UserPurchasePageReqVO vo) {
        DataResult<PageVO<UserPurchase>> result = DataResult.success();
        result.setData(userPurchaseService.pageInfo(vo));
        return result;
    }

}
