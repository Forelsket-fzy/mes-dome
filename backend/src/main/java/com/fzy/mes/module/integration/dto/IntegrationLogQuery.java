package com.fzy.mes.module.integration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class IntegrationLogQuery {

	private String bizType;
	private String targetSystem;
	@Min(0)
	@Max(3)
	private Integer status;
	@Min(1)
	private long current = 1;
	@Min(1)
	@Max(100)
	private long size = 10;

}
