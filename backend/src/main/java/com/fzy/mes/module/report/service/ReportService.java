package com.fzy.mes.module.report.service;

import com.fzy.mes.module.report.dto.ReportSubmitRequest;
import com.fzy.mes.module.report.vo.DefectReasonVO;
import com.fzy.mes.module.report.vo.ReportValidateResponse;

import java.util.List;

public interface ReportService {

	/**
	 * D22：同步校验报工请求（数量 / 状态 / 权限 / 不良明细）。
	 * D23 将在此之后接入 Redis 幂等、分布式锁与 MQ。
	 */
	ReportValidateResponse validateSubmit(ReportSubmitRequest request, Long operatorId, String requestId);

	List<DefectReasonVO> listDefectReasons(String operationCode);

}
