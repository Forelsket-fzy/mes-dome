package com.fzy.mes.module.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkOrderDetailVO {

    private Long id;
    private String erpOrderNo;
    private String productCode;
    private String productName;
    private Integer planQty;
    private Integer completedQty;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private String erpStatus;
    private Integer version;
    private String cancelReason;
    private LocalDateTime updatedAt;
    private List<OperationTaskVO> operations;

}
