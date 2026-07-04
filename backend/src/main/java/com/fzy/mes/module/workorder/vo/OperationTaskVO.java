package com.fzy.mes.module.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationTaskVO {

    private Long id;
    private Integer seq;
    private String operationCode;
    private String operationName;
    private Integer planQty;
    private Integer completedQty;
    private Integer status;
    private Integer priority;
    private LocalDateTime plannedStart;
    private Long assignedTo;

}
