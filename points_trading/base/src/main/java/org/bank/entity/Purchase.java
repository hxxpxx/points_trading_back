package org.bank.entity;

import java.io.Serializable;
import java.util.Date;

public class Purchase implements Serializable {
    private String purchaseNo;

    private String financialId;

    private String title;

    private Integer num;

    private Double price;

    private Double totalAmout;

    private String userId;

    private String status;

    private Date createtime;

    private Date paytime;

    private Date updatetime;

    private Financial financial;

    private PurchaseSnapshot purchaseSnapshot;

    public Financial getFinancial() {
        return financial;
    }

    public void setFinancial(Financial financial) {
        this.financial = financial == null ? null :financial;
    }

    public PurchaseSnapshot getPurchaseSnapshot() {
        return purchaseSnapshot;
    }

    public void setPurchaseSnapshot(PurchaseSnapshot purchaseSnapshot) {
        this.purchaseSnapshot = purchaseSnapshot == null ? null :purchaseSnapshot;
    }

    private static final long serialVersionUID = 1L;


    public String getPurchaseNo() {
        return purchaseNo;
    }

    public void setPurchaseNo(String purchaseNo) {
        this.purchaseNo = purchaseNo == null ? null : purchaseNo.trim();
    }

    public String getFinancialId() {
        return financialId;
    }

    public void setFinancialId(String financialId) {
        this.financialId = financialId == null ? null : financialId.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getTotalAmout() {
        return totalAmout;
    }

    public void setTotalAmout(Double totalAmout) {
        this.totalAmout = totalAmout;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getPaytime() {
        return paytime;
    }

    public void setPaytime(Date paytime) {
        this.paytime = paytime;
    }

    public Date getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(Date updatetime) {
        this.updatetime = updatetime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", purchaseNo=").append(purchaseNo);
        sb.append(", financialId=").append(financialId);
        sb.append(", title=").append(title);
        sb.append(", num=").append(num);
        sb.append(", price=").append(price);
        sb.append(", totalAmout=").append(totalAmout);
        sb.append(", userId=").append(userId);
        sb.append(", status=").append(status);
        sb.append(", createtime=").append(createtime);
        sb.append(", paytime=").append(paytime);
        sb.append(", updatetime=").append(updatetime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}