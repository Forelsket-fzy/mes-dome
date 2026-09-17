package com.fzy.mes.module.integration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.integration.dto.IntegrationLogQuery;
import com.fzy.mes.module.integration.entity.IntegrationLog;
import com.fzy.mes.module.integration.service.IntegrationLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

	@Autowired
	private IntegrationLogService integrationLogService;

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@GetMapping("/logs")
	public Result<Page<IntegrationLog>> page(@Valid IntegrationLogQuery query) {
		return Result.success(integrationLogService.page(query));
	}

	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
	@PostMapping("/logs/{id}/retry")
	public Result<IntegrationLog> retry(@PathVariable Long id) {
		return Result.success(integrationLogService.retry(id));
	}

}
