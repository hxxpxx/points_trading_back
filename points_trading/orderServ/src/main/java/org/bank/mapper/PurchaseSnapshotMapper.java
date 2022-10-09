package org.bank.mapper;

import org.bank.entity.PurchaseSnapshot;

public interface PurchaseSnapshotMapper {
    int deleteByPrimaryKey(String purchaseNo);

    int insert(PurchaseSnapshot record);

    int insertSelective(PurchaseSnapshot record);

    PurchaseSnapshot selectByPrimaryKey(String purchaseNo);

    int updateByPrimaryKeySelective(PurchaseSnapshot record);

    int updateByPrimaryKey(PurchaseSnapshot record);
}