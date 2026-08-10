package com.fzy.mes.module.report.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportValidateResponse {

	/** D22：仅表示同步校验通过；D23 起才会真正受理并返回 reportId */
	private boolean validated;
	private String requestId;
	private Long taskId;
	private Long workOrderId;
	private Integer reportQty;
	private Integer remainingQty;
	private String message;

}
