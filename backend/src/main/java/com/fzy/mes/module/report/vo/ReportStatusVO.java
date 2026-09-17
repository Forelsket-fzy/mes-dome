package com.fzy.mes.module.report.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportStatusVO {

	private Long reportId;
	private String requestId;
	private Long taskId;
	private Integer goodQty;
	private Integer defectQty;
	/** 0处理中 / 1成功 / 2失败 */
	private Integer status;
	private String errorMsg;
	private LocalDateTime reportedAt;

}
