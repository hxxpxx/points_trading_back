package org.bank.vo.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PurchaseAddReqVO {
    @NotBlank(message = "订单号不能为空")
    private String purchaseNo;

    @NotBlank(message = "商品不能为空")
    private String financialId;

    private String title;

    @NotNull
    private Integer num;

    private Double price;

    private Double totalAmout;

    private String userId;

}
