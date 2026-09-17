package com.fzy.mes.module.erp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;
import com.fzy.mes.module.erp.service.ErpWorkOrderService;
import com.fzy.mes.module.erp.vo.ErpCloseWorkOrderResponse;
import com.fzy.mes.module.integration.dto.IntegrationLogQuery;
import com.fzy.mes.module.integration.entity.IntegrationLog;
import com.fzy.mes.module.integration.service.IntegrationLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ErpWorkOrderController {

	@Autowired
	private ErpWorkOrderService erpWorkOrderService;
	@Autowired
	private IntegrationLogService integrationLogService;

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@PostMapping("/erp/work-orders")
	public Result<ErpPushWorkOrderResponse> pushWorkOrder(
			@RequestBody @Valid ErpPushWorkOrderRequest erpPushWorkOrderRequest) {
		return Result.success(erpWorkOrderService.push(erpPushWorkOrderRequest));
	}

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@PostMapping("/erp/work-orders/{erpOrderNo}/close")
	public Result<ErpCloseWorkOrderResponse> closeWorkOrder(
			@PathVariable String erpOrderNo,
			@RequestBody @Valid ErpCloseWorkOrderRequest req) {
		return Result.success(erpWorkOrderService.close(erpOrderNo, req));
	}

	/** D26+：回传对账别名，等同 GET /api/integration/logs?bizType=REPORT_CALLBACK */
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@GetMapping("/erp/callback-logs")
	public Result<Page<IntegrationLog>> callbackLogs(@Valid IntegrationLogQuery query) {
		if (query.getBizType() == null || query.getBizType().isBlank()) {
			query.setBizType("REPORT_CALLBACK");
		}
		return Result.success(integrationLogService.page(query));
	}

}
