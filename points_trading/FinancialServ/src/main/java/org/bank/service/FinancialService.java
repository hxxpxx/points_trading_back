package org.bank.service;

import org.bank.entity.Financial;
import org.bank.vo.req.FinancialAddReqVO;
import org.bank.vo.req.FinancialDeductStockReqVO;
import org.bank.vo.req.FinancialPageReqVO;
import org.bank.vo.req.FinancialUpdateReqVO;
import org.bank.vo.resp.PageVO;

import java.util.List;

public interface FinancialService {
    Financial addFinancial(FinancialAddReqVO vo);

    Integer getStock(String financialId);

    boolean lockStock(Integer stock,String financialId);

    boolean unLockStock(Integer stock,String financialId);

    void updateFinancial(FinancialUpdateReqVO vo);

    void deductStock(FinancialDeductStockReqVO vo);

    Financial detailInfo(String id);

    void deletedFinancial(String id);

    PageVO<Financial> pageInfo(FinancialPageReqVO vo);

    List<Financial> selectAllFinancial();
}
