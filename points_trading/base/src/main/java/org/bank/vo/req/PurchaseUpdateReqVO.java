package org.bank.vo.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
public class PurchaseUpdateReqVO {

    @NotBlank(message = "订单号不能为空")
    private String purchaseNo;


    private String financialId;


    private String title;


    private Integer num;


    private Double price;


    private Double totalAmout;


    private String userId;


    private Date paytime;

    private Date updatetime;

    private String status;
}
