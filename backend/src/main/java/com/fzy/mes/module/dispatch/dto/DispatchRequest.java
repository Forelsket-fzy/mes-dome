package com.fzy.mes.module.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DispatchRequest {

    @NotNull(message = "工序任务ID不能为空")
    private Long taskId;

    @NotNull(message = "被派工人ID不能为空")
    private Long assigneeId;

}
