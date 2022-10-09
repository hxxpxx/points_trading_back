package org.bank.entity;

import java.io.Serializable;
import java.util.Date;

public class Order implements Serializable {
    private String orderNo;

    private String goodsId;

    private Integer addressId;

    private String title;

    private Integer num;

    private Double price;

    private Double totalAmout;

    private String userId;

    private String status;

    private Date createtime;

    private Date paytime;

    private Date updatetime;

    private Goods goods;

    private OrderAddress orderAddress;

    private OrderSnapshot orderSnapshot;

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods == null ? null :goods;
    }

    public OrderAddress getOrderAddress() {
        return orderAddress;
    }

    public void setOrderAddress(OrderAddress orderAddress) {
        this.orderAddress = orderAddress == null ? null :orderAddress;
    }

    public OrderSnapshot getOrderSnapshot() {
        return orderSnapshot;
    }

    public void setOrderSnapshot(OrderSnapshot orderSnapshot) {
        this.orderSnapshot = orderSnapshot == null ? null :orderSnapshot;
    }

    private static final long serialVersionUID = 1L;

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId == null ? null : addressId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo == null ? null : orderNo.trim();
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId == null ? null : goodsId.trim();
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
        sb.append(", orderNo=").append(orderNo);
        sb.append(", goodsId=").append(goodsId);
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