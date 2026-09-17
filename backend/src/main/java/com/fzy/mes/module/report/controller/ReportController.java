package com.fzy.mes.module.report.controller;

import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.auth.vo.LoginUser;
import com.fzy.mes.module.report.dto.ReportSubmitRequest;
import com.fzy.mes.module.report.service.ReportService;
import com.fzy.mes.module.report.vo.DefectReasonVO;
import com.fzy.mes.module.report.vo.ReportAcceptResponse;
import com.fzy.mes.module.report.vo.ReportStatusVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {

	@Autowired
	private ReportService reportService;

	/**
	 * D23：同步校验 + Redis 幂等 + 任务锁 + 发 MQ，返回 202 已受理。
	 */
	@PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_ADMIN')")
	@PostMapping("/reports")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Result<ReportAcceptResponse> submit(
			@RequestHeader(value = "X-Request-Id", required = false) String requestId,
			@Valid @RequestBody ReportSubmitRequest request,
			@AuthenticationPrincipal LoginUser user) {
		return Result.accepted(reportService.submit(request, user.getId(), requestId));
	}

	@PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_ADMIN','ROLE_PLANNER')")
	@GetMapping("/reports/{reportId}")
	public Result<ReportStatusVO> getById(@PathVariable Long reportId) {
		return Result.success(reportService.getById(reportId));
	}

	@PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_ADMIN','ROLE_PLANNER','ROLE_QC')")
	@GetMapping("/defect-reasons")
	public Result<List<DefectReasonVO>> listDefectReasons(
			@RequestParam(value = "operationCode", required = false) String operationCode) {
		return Result.success(reportService.listDefectReasons(operationCode));
	}

}
