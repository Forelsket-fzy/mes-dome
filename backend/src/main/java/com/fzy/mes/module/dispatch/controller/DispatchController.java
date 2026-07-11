package com.fzy.mes.module.dispatch.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.dispatch.dto.WorkerQuery;
import com.fzy.mes.module.dispatch.service.DispatchService;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_PLANNER')")
    @GetMapping("/workers")
    public Result<Page<WorkerListItemVO>> pageWorkers(@Valid WorkerQuery query) {
        return Result.success(dispatchService.pageWorkers(query));
    }

}
