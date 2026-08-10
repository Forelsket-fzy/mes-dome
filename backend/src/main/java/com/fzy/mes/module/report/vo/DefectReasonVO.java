package com.fzy.mes.module.report.vo;

import lombok.Data;

@Data
public class DefectReasonVO {

	private Long id;
	private String code;
	private String name;
	private String defectType;
	private String operationCode;

}
