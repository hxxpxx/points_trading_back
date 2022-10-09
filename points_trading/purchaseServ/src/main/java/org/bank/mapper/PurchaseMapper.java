package org.bank.mapper;


import org.bank.entity.Purchase;

import java.util.List;

public interface PurchaseMapper {
    int deleteByPrimaryKey(String purchaseNo);

    int insert(Purchase record);

    int insertSelective(Purchase record);

    Purchase selectByPrimaryKey(String purchaseNo);

    int updateByPrimaryKeySelective(Purchase record);

    int updateByPrimaryKey(Purchase record);

    List<Purchase> selectAll(Purchase purchase);
}