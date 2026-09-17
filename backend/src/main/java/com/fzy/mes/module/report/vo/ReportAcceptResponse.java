package com.fzy.mes.module.report.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportAcceptResponse {

	private Long reportId;
	private String requestId;
	/** 0处理中 / 1成功 / 2失败 */
	private Integer status;
	private String message;

}
