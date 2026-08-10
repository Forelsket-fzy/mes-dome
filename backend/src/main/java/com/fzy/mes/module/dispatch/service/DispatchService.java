package com.fzy.mes.module.dispatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.dispatch.dto.AuditQuery;
import com.fzy.mes.module.dispatch.dto.DispatchRequest;
import com.fzy.mes.module.dispatch.dto.WorkerQuery;
import com.fzy.mes.module.dispatch.vo.AuditResponse;
import com.fzy.mes.module.dispatch.vo.DispatchResponse;
import com.fzy.mes.module.dispatch.vo.MyTaskItemVO;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import jakarta.validation.Valid;

import java.util.List;

public interface DispatchService {

    Page<WorkerListItemVO> pageWorkers(WorkerQuery query);

    DispatchResponse dispatch(@Valid DispatchRequest request, Long operatorId);

    Page<AuditResponse> pageAudits(@Valid AuditQuery query);

    List<MyTaskItemVO> listMyTasks(Long workerId);
}
