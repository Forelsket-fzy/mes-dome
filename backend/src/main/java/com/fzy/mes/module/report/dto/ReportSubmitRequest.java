package com.fzy.mes.module.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportSubmitRequest {

	@NotNull(message = "工序任务ID不能为空")
	private Long taskId;

	@NotNull(message = "良品数量不能为空")
	@Min(value = 0, message = "良品数量不能为负数")
	private Integer goodQty;

	@NotNull(message = "不良品数量不能为空")
	@Min(value = 0, message = "不良品数量不能为负数")
	private Integer defectQty;

	@Valid
	private List<DefectItemRequest> defects = new ArrayList<>();

}
