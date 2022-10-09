package org.bank.mapper;


import org.bank.entity.UserPurchase;

import java.util.List;

public interface UserPurchaseMapper {
    int deleteByPrimaryKey(String userPurchaseNo);

    int insert(UserPurchase record);

    int insertSelective(UserPurchase record);

    UserPurchase selectByPrimaryKey(String userPurchaseNo);

    int updateByPrimaryKeySelective(UserPurchase record);

    int updateByPrimaryKey(UserPurchase record);

    List<UserPurchase> selectAll(UserPurchase userPurchase);
}