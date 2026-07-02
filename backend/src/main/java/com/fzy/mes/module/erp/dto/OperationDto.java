package com.fzy.mes.module.erp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationDto {

    @NotBlank(message = "工序编码不能为空")
    @Size(max = 30, message = "工序编码长度不能超过30")
    private String operationCode;

    @Size(max = 100, message = "工序名称长度不能超过100")
    private String operationName;

    @NotNull(message = "工序序号不能为空")
    @Min(value = 1, message = "工序序号至少为1")
    private Integer seq;

    private Integer priority;

    private LocalDateTime plannedStart;

}
