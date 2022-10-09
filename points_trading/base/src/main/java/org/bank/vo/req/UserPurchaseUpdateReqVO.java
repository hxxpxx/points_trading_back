package org.bank.vo.req;

import lombok.Data;
import java.util.Date;

@Data
public class UserPurchaseUpdateReqVO {

    private String userId;

    private String financialId;

    private Integer holdNum;

    private Date updatetime;
}
