package com.fzy.mes.module.workorder.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.workorder.dto.WorkOrderQuery;
import com.fzy.mes.module.workorder.service.WorkOrderService;
import com.fzy.mes.module.workorder.vo.WorkOrderDetailVO;
import com.fzy.mes.module.workorder.vo.WorkOrderListItemVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @PreAuthorize("hasAllRoles('ROLE_ADMIN','ROLE_PLANNER')")
    @GetMapping("/work-orders")
    public Result<Page<WorkOrderListItemVO>> page(@Valid WorkOrderQuery query)  {
        return Result.success(workOrderService.pageList(query));
    }

    @PreAuthorize("hasAllRoles('ROLE_ADMIN','ROLE_PLANNER')")
    @GetMapping("/work-orders/{id}")
    public Result<WorkOrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(workOrderService.getDetail(id));
    }

}
