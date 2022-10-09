//package org.bank.service;
//
//import org.bank.entity.Purchase;
//import org.bank.entity.PurchaseSnapshot;
//import org.bank.vo.req.PurchaseAddReqVO;
//import org.bank.vo.req.PurchasePageReqVO;
//import org.bank.vo.req.PurchaseUpdateReqVO;
//import org.bank.vo.resp.PageVO;
//
//public interface PurchaseService {
//
//    String issuePurchaseNo();
//
//    boolean cancelPurchaseNo(String purchaseNo);
//
//    Purchase addPurchase(PurchaseAddReqVO vo,String userId);
//
//    void canclePurchase(String purchaseNo);
//
//    void canclePurchase(String purchaseNo,String userId);
//
//    void payPurchase(String purchaseNo,String userId);
//
//    void updatePurchase(PurchaseUpdateReqVO vo);
//
//    Purchase detailInfo(String purchaseNo,String userId);
//
//    Purchase detailInfo(String purchaseNo);
//
//    PageVO<Purchase> pageInfo(PurchasePageReqVO vo);
//
//    PurchaseSnapshot getPurchaseSnapshotForPurchaseNo(String purchaseNo);
//
//}
