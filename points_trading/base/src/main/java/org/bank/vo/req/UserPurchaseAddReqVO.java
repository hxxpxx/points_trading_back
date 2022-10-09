package org.bank.vo.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserPurchaseAddReqVO {
    @NotBlank(message = "用户不能为空")
    private String userId;

    @NotBlank(message = "商品不能为空")
    private String financialId;

    private String title;

    @NotBlank(message = "持有份额")
    private Integer holdNum;
}
