package com.fzy.mes.module.dispatch.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditResponse {

    private Long auditId;
    private Long operatorId;
    private String operatorName;
    private Long dispatchId;
    private Long taskId;
    private Long workOrderId;
    private String erpOrderNo;
    private String operationName;
    private String operationCode;
    private Long assigneeId;
    private String assigneeName;
    private String snapshotJson;

    private LocalDateTime createdAt;
    private Integer mode;


}
