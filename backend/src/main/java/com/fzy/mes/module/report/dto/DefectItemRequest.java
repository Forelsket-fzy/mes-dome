package com.fzy.mes.module.report.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DefectItemRequest {

	@NotNull(message = "不良原因ID不能为空")
	private Long reasonId;

	@NotNull(message = "不良数量不能为空")
	@Min(value = 1, message = "不良数量至少为1")
	private Integer qty;

}
