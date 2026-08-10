package com.fzy.mes.module.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.dispatch.dto.AuditQuery;
import com.fzy.mes.module.dispatch.dto.DispatchRequest;
import com.fzy.mes.module.dispatch.dto.SnapshotDTO;
import com.fzy.mes.module.dispatch.dto.WorkerQuery;
import com.fzy.mes.module.dispatch.entity.DispatchAuditLog;
import com.fzy.mes.module.dispatch.entity.DispatchRecord;
import com.fzy.mes.module.dispatch.mapper.DispatchAuditLogMapper;
import com.fzy.mes.module.dispatch.mapper.DispatchRecordMapper;
import com.fzy.mes.module.dispatch.mapper.DispatchWorkerMapper;
import com.fzy.mes.module.dispatch.service.DispatchService;
import com.fzy.mes.module.dispatch.vo.AuditResponse;
import com.fzy.mes.module.dispatch.vo.DispatchResponse;
import com.fzy.mes.module.dispatch.vo.MyTaskItemVO;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderEvent;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class DispatchServiceImpl implements DispatchService {

    private static final int TASK_STATUS_PENDING = 0;
    private static final int TASK_STATUS_COMPLETED = 2;
    private static final int DISPATCH_MODE_MANUAL = 1;

    @Autowired
    private DispatchWorkerMapper dispatchWorkerMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private OperationTaskMapper operationTaskMapper;
    @Autowired
    private WorkOrderStateMachine workOrderStateMachine;
    @Autowired
    private DispatchRecordMapper dispatchRecordMapper;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DispatchAuditLogMapper dispatchAuditLogMapper;

    @Override
    public Page<WorkerListItemVO> pageWorkers(WorkerQuery query) {
        Page<WorkerListItemVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        return dispatchWorkerMapper.selectWorkerPage(page, query.getSkillLevel(), keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DispatchResponse dispatch(DispatchRequest request, Long operatorId) {
        if (!dispatchWorkerMapper.existsActiveWorker(request.getAssigneeId())) {
            throw new BusinessException("被派工人不存在或非工人角色");
        }

        SnapshotDTO snapshotDTO = new SnapshotDTO();
        OperationTask task = operationTaskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new BusinessException("工序任务不存在");
        }
        if (task.getStatus() != null && task.getStatus() == TASK_STATUS_COMPLETED) {
            throw new BusinessException("工序任务已完工，不可派工");
        }
        if (task.getAssignedTo() != null) {
            throw new BusinessException("工序任务已派工");
        }
        if (task.getStatus() != null && task.getStatus() != TASK_STATUS_PENDING) {
            throw new BusinessException("工序任务状态不支持派工");
        }

        snapshotDTO.setTaskId(task.getId());
        snapshotDTO.setWorkOrderId(task.getWorkOrderId());
        snapshotDTO.setOperationCode(task.getOperationCode());
        snapshotDTO.setAssignedToBefore(task.getAssignedTo());


        WorkOrder workOrder = workOrderMapper.selectById(task.getWorkOrderId());
        if (workOrder == null) {
            throw new BusinessException("工单不存在");
        }

        WorkOrderStatus currentStatus = WorkOrderStatus.fromCode(workOrder.getStatus());
        if (currentStatus.isFinal() || currentStatus == WorkOrderStatus.COMPLETED) {
            throw new BusinessException("工单当前状态不支持派工");
        }
        if (currentStatus != WorkOrderStatus.ISSUED && currentStatus != WorkOrderStatus.ASSIGNED) {
            throw new BusinessException("工单当前状态不支持派工");
        }

        snapshotDTO.setWorkOrderStatusBefore(currentStatus.getCode());
        snapshotDTO.setErpOrderNo(workOrder.getErpOrderNo());

        int nextWorkOrderStatus = workOrder.getStatus();
        if (currentStatus == WorkOrderStatus.ISSUED) {
            WorkOrderStatus nextStatus = workOrderStateMachine.transit(currentStatus, WorkOrderEvent.DISPATCH);
            nextWorkOrderStatus = nextStatus.getCode();

            UpdateWrapper<WorkOrder> workOrderUpdate = new UpdateWrapper<>();
            workOrderUpdate.set("status", nextWorkOrderStatus)
                    .eq("id", workOrder.getId())
                    .eq("version", workOrder.getVersion());
            if (workOrderMapper.update(null, workOrderUpdate) == 0) {
                throw new BusinessException("并发更新失败，请重试");
            }
            cacheService.deleteKey(RedisCacheKeys.WORK_ORDER_STATUS_STATS);
        }

        UpdateWrapper<OperationTask> taskUpdate = new UpdateWrapper<>();
        taskUpdate.set("assigned_to", request.getAssigneeId())
                .isNull("assigned_to")
                .eq("id", task.getId())
                .eq("version", task.getVersion());
        if (operationTaskMapper.update(null, taskUpdate) == 0) {
            throw new BusinessException("并发更新失败，请重试");
        }

        DispatchRecord record = new DispatchRecord();
        record.setTaskId(task.getId());
        record.setOperatorId(operatorId);
        record.setAssigneeId(request.getAssigneeId());
        record.setMode(DISPATCH_MODE_MANUAL);
        dispatchRecordMapper.insert(record);

        snapshotDTO.setMode(DISPATCH_MODE_MANUAL);
        snapshotDTO.setAssigneeIdAfter(request.getAssigneeId());
        String json = objectMapper.writeValueAsString(snapshotDTO);
        DispatchAuditLog dispatchAuditLog = new DispatchAuditLog();
        dispatchAuditLog.setDispatchId(record.getId());
        dispatchAuditLog.setSnapshotJson(json);
        dispatchAuditLog.setActionBy(operatorId);
        dispatchAuditLog.setCreatedAt(LocalDateTime.now());
        dispatchAuditLogMapper.insert(dispatchAuditLog);

        DispatchResponse response = new DispatchResponse();
        response.setDispatchId(record.getId());
        response.setTaskId(task.getId());
        response.setWorkOrderId(task.getWorkOrderId());
        response.setWorkOrderStatus(nextWorkOrderStatus);
        return response;
    }

    @Override
    public Page<AuditResponse> pageAudits(AuditQuery query) {
        Page<AuditResponse> page = new Page<>(query.getPageNum(), query.getPageSize());
        return dispatchAuditLogMapper.selectAuditPage(
                page, query.getTaskId(), query.getAssigneeId(), query.getOperatorId());
    }

    @Override
    public List<MyTaskItemVO> listMyTasks(Long workerId) {
        if (workerId == null) {
            return Collections.emptyList();
        }
        List<MyTaskItemVO> tasks = operationTaskMapper.selectMyTasks(workerId);
        return tasks == null ? Collections.emptyList() : tasks;
    }

}
