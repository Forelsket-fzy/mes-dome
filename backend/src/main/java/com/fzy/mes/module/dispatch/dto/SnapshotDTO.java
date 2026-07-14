package com.fzy.mes.module.dispatch.dto;

import lombok.Data;

@Data
public class SnapshotDTO {

    private Long taskId;
    private Long workOrderId;
    private String erpOrderNo;
    private String operationCode;
    private Long assignedToBefore;
    private Integer workOrderStatusBefore;
    private Long assigneeIdAfter;
    private Integer mode;

}
