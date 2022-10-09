package org.bank.vo.req;


import lombok.Data;

@Data
public class FinancialPageReqVO {
    /**
     * 第几页
     */
    private int pageNum=1;

    /**
     *分页数量
     */
    private int pageSize=10;

    /**
     * 金融产品名称
     */
    private String name;



}
