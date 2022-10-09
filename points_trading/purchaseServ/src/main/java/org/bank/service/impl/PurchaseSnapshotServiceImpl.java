package org.bank.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bank.entity.PurchaseSnapshot;
import org.bank.exception.BusinessException;
import org.bank.exception.code.BaseResponseCode;
import org.bank.mapper.PurchaseSnapshotMapper;
import org.bank.service.PurchaseSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PurchaseSnapshotServiceImpl implements PurchaseSnapshotService {
    @Autowired
    PurchaseSnapshotMapper purchaseSnapshotMapper;

    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = {Exception.class})
    @Override
    public PurchaseSnapshot addPurchaseSnapshot(PurchaseSnapshot vo) {
        int insertSize= purchaseSnapshotMapper.insertSelective(vo);
        if(insertSize==0){
            throw new BusinessException(BaseResponseCode.OPERATION_ERRO);
        }
        return vo;
    }
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = {Exception.class})
    @Override
    public PurchaseSnapshot detailInfo(String purchaseNo) {
        PurchaseSnapshot purchaseSnapshot=purchaseSnapshotMapper.selectByPrimaryKey(purchaseNo);
        if(purchaseSnapshot==null){
            log.error("传入 的 purchaseNo:{}不合法",purchaseNo);
            throw new BusinessException(BaseResponseCode.DATA_ERROR);
        }
        return purchaseSnapshot;
    }

    @Override
    public void deletePurchaseSnapshot(String purchaseNo) {

    }
}
