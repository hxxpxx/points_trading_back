package org.bank.service.impl;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.bank.entity.UserPurchase;
import org.bank.mapper.UserPurchaseMapper;
import org.bank.service.UserPurchaseService;
import org.bank.utils.PageUtils;
import org.bank.vo.req.UserPurchaseAddReqVO;
import org.bank.vo.req.UserPurchasePageReqVO;
import org.bank.vo.req.UserPurchaseUpdateReqVO;
import org.bank.vo.resp.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserPurchaseServiceImpl implements UserPurchaseService {

    @Autowired
    UserPurchaseMapper userPurchaseMapper;


    @Override
    public UserPurchase addPurchase(UserPurchaseAddReqVO vo, String userId) {

        return null;
    }

    @Override
    public void updatePurchase(UserPurchaseUpdateReqVO vo) {

    }

    @Override
    public PageVO<UserPurchase> pageInfo(UserPurchasePageReqVO vo) {
        PageHelper.startPage(vo.getPageNum(), vo.getPageSize());
        UserPurchase query = new UserPurchase();
        BeanUtils.copyProperties(vo, query);
        List<UserPurchase> userPurchase = userPurchaseMapper.selectAll(query);
        return PageUtils.getPageVO(userPurchase);
    }

    @Override
    public List<UserPurchase> selectAllUserPurchase() {
        return userPurchaseMapper.selectAll(null);
    }

}
