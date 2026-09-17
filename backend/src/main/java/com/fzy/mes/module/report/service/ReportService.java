package com.fzy.mes.module.report.service;

import com.fzy.mes.module.report.dto.ReportSubmitRequest;
import com.fzy.mes.module.report.vo.DefectReasonVO;
import com.fzy.mes.module.report.vo.ReportAcceptResponse;
import com.fzy.mes.module.report.vo.ReportStatusVO;
import com.fzy.mes.module.report.vo.ReportValidateResponse;

import java.util.List;

public interface ReportService {

	/**
	 * D22：同步校验报工请求（数量 / 状态 / 权限 / 不良明细）。
	 */
	ReportValidateResponse validateSubmit(ReportSubmitRequest request, Long operatorId, String requestId);

	/**
	 * D23：校验 + Redis 幂等 + Redisson 锁 + 落处理中单据 + 发 MQ → 已受理。
	 */
	ReportAcceptResponse submit(ReportSubmitRequest request, Long operatorId, String requestId);

	ReportStatusVO getById(Long reportId);

	List<DefectReasonVO> listDefectReasons(String operationCode);

}
