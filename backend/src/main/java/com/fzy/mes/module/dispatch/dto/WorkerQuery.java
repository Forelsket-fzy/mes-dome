package com.fzy.mes.module.dispatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WorkerQuery {

    @Min(value = 1, message = "pageNum 至少为 1")
    private int pageNum = 1;

    @Min(value = 1, message = "pageSize 至少为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private int pageSize = 20;

    @Min(value = 1, message = "skillLevel 最小为 1")
    @Max(value = 5, message = "skillLevel 最大为 5")
    private Integer skillLevel;

    private String keyword;

}
