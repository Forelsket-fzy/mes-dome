package com.fzy.mes.module.dispatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AuditQuery {

    @Min(value = 1, message = "pageNum 至少为 1")
    private int pageNum = 1;

    @Min(value = 1, message = "pageSize 至少为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private int pageSize = 20;

    private Long taskId;
    private Long assigneeId;
    private Long operatorId;

}
