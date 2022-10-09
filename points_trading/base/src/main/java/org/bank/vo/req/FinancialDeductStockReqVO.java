package org.bank.vo.req;


import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FinancialDeductStockReqVO {
    /**
     * 金融产品ID
     */
    @NotBlank(message = "金融产品Id不能为空")
    private String id;
    @NotBlank(message = "金融产品库存不能为空")
    private Integer stock;

}
