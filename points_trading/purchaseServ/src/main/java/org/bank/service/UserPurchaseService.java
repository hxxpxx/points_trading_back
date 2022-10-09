package org.bank.service;

import org.bank.entity.UserPurchase;
import org.bank.vo.req.UserPurchaseAddReqVO;
import org.bank.vo.req.UserPurchasePageReqVO;
import org.bank.vo.req.UserPurchaseUpdateReqVO;
import org.bank.vo.resp.PageVO;

import java.util.List;

public interface UserPurchaseService {

    UserPurchase addPurchase(UserPurchaseAddReqVO vo, String userId);

    void updatePurchase(UserPurchaseUpdateReqVO vo);

    PageVO<UserPurchase> pageInfo(UserPurchasePageReqVO vo);

    public List<UserPurchase> selectAllUserPurchase();
}
