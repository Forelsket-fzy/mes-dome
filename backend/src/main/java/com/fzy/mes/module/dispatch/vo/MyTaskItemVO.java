package com.fzy.mes.module.dispatch.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyTaskItemVO {

	private Long taskId;
	private Long workOrderId;
	private String erpOrderNo;
	private String productCode;
	private String productName;
	private String operationCode;
	private String operationName;
	private Integer seq;
	private Integer planQty;
	private Integer completedQty;
	private Integer taskStatus;
	private Integer workOrderStatus;
	private Integer priority;
	private LocalDateTime plannedStart;

}
