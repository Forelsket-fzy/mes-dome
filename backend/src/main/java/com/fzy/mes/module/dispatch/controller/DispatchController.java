package com.fzy.mes.module.dispatch.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.auth.vo.LoginUser;
import com.fzy.mes.module.dispatch.dto.AuditQuery;
import com.fzy.mes.module.dispatch.dto.DispatchRequest;
import com.fzy.mes.module.dispatch.dto.WorkerQuery;
import com.fzy.mes.module.dispatch.service.DispatchService;
import com.fzy.mes.module.dispatch.vo.AuditResponse;
import com.fzy.mes.module.dispatch.vo.DispatchResponse;
import com.fzy.mes.module.dispatch.vo.MyTaskItemVO;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
    @GetMapping("/workers")
    public Result<Page<WorkerListItemVO>> pageWorkers(@Valid WorkerQuery query) {
        return Result.success(dispatchService.pageWorkers(query));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
    @PostMapping("/dispatch")
    public Result<DispatchResponse> dispatch(@Valid @RequestBody DispatchRequest request,
                                             @AuthenticationPrincipal LoginUser user) {
        return Result.success(dispatchService.dispatch(request, user.getId()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PLANNER')")
    @GetMapping("/dispatch/audit")
    public Result<Page<AuditResponse>> pageAudits(@Valid AuditQuery query) {
        return Result.success(dispatchService.pageAudits(query));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_ADMIN')")
    @GetMapping("/tasks/my")
    public Result<List<MyTaskItemVO>> myTasks(@AuthenticationPrincipal LoginUser user) {
        return Result.success(dispatchService.listMyTasks(user.getId()));
    }

}
