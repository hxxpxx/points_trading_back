package org.bank.vo.req;


import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class FinancialUpdateReqVO {
    /**
     * 金融产品ID
     */
    @NotBlank(message = "金融产品Id不能为空")
    private String id;

    /**
     * 金融产品名称
     */
    @NotBlank(message = "金融产品名称不能为空")
    private String name;

    /**
     * 金融产品描述
     */
    private String describe;

    /**
     *金融产品价格
     */
    @NotNull(message = "金融产品价格不能为空")
    private Double price;

    private Integer totalstock;

    private Integer stock;

    private Integer deleted;
}
