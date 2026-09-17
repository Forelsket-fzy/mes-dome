package com.fzy.mes.module.report.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 报工提交 MQ 消息体（Topic: mes_report_submit）。
 */
@Data
public class ReportSubmitMessage {

	private Long reportId;
	private String requestId;
	private Long taskId;
	private Long workOrderId;
	private Long operatorId;
	private Integer goodQty;
	private Integer defectQty;
	private List<DefectItemRequest> defects = new ArrayList<>();

}
