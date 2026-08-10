package com.fzy.mes.module.erp.controller;

import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;
import com.fzy.mes.module.erp.service.ErpWorkOrderService;
import com.fzy.mes.module.erp.vo.ErpCloseWorkOrderResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

}
