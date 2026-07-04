package com.fzy.mes.module.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderListItemVO {

    private Long id;
    private String erpOrderNo;
    private String productCode;
    private String productName;
    private Integer planQty;
    private Integer completedQty;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;

}
