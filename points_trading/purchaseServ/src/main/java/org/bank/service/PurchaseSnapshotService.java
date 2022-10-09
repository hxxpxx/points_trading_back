package org.bank.service;

import org.bank.entity.PurchaseSnapshot;

public interface PurchaseSnapshotService {
    PurchaseSnapshot addPurchaseSnapshot(PurchaseSnapshot vo);

    PurchaseSnapshot detailInfo(String purchaseNo);

    void deletePurchaseSnapshot(String purchaseNo);
}
