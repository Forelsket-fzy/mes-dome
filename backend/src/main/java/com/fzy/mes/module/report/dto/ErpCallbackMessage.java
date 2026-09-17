package com.fzy.mes.module.report.dto;

import lombok.Data;

/**
 * ERP 回传 MQ 消息体（Topic: mes_erp_callback）。
 */
@Data
public class ErpCallbackMessage {

	private Long reportId;
	private String requestId;
	private Long workOrderId;
	private String erpOrderNo;
	private Long taskId;
	private Integer goodQty;
	private Integer defectQty;
	private Integer workOrderStatus;
	private Boolean completed;

}
