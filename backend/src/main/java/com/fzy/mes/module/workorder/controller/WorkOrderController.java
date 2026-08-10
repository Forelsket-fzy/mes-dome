package com.fzy.mes.module.workorder.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.workorder.dto.WorkOrderQuery;
import com.fzy.mes.module.workorder.service.WorkOrderService;
import com.fzy.mes.module.workorder.vo.WorkOrderDetailVO;
import com.fzy.mes.module.workorder.vo.WorkOrderListItemVO;
import com.fzy.mes.module.workorder.vo.WorkOrderStatusStatsItemsVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkOrderController {

	@Autowired
	private WorkOrderService workOrderService;

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@GetMapping("/work-orders")
	public Result<Page<WorkOrderListItemVO>> page(@Valid WorkOrderQuery query) {
		return Result.success(workOrderService.pageList(query));
	}

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@GetMapping("/work-orders/stats")
	public Result<WorkOrderStatusStatsItemsVO> stats() {
		return Result.success(workOrderService.getStats());
	}

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@GetMapping("/work-orders/{id}")
	public Result<WorkOrderDetailVO> detail(@PathVariable Long id) {
		return Result.success(workOrderService.getDetail(id));
	}

}
