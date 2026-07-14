package com.fzy.mes.module.dispatch.service;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.dispatch.dto.DispatchRequest;
import com.fzy.mes.module.dispatch.entity.DispatchRecord;
import com.fzy.mes.module.dispatch.mapper.DispatchRecordMapper;
import com.fzy.mes.module.dispatch.mapper.DispatchWorkerMapper;
import com.fzy.mes.module.dispatch.service.impl.DispatchServiceImpl;
import com.fzy.mes.module.dispatch.vo.DispatchResponse;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderEvent;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private DispatchWorkerMapper dispatchWorkerMapper;
    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private OperationTaskMapper operationTaskMapper;
    @Mock
    private WorkOrderStateMachine workOrderStateMachine;
    @Mock
    private DispatchRecordMapper dispatchRecordMapper;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private DispatchServiceImpl dispatchService;

    @Test
    void dispatchShouldMoveWorkOrderToAssignedOnFirstDispatch() {
        when(dispatchWorkerMapper.existsActiveWorker(3L)).thenReturn(true);

        OperationTask task = buildTask(1L, 10L, 0, null, 0);
        WorkOrder workOrder = buildWorkOrder(10L, 0, 0);
        when(operationTaskMapper.selectById(1L)).thenReturn(task);
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder);
        when(workOrderStateMachine.transit(WorkOrderStatus.ISSUED, WorkOrderEvent.DISPATCH))
                .thenReturn(WorkOrderStatus.ASSIGNED);
        when(workOrderMapper.update(isNull(), any())).thenReturn(1);
        when(operationTaskMapper.update(isNull(), any())).thenReturn(1);
        when(dispatchRecordMapper.insert(any(DispatchRecord.class))).thenAnswer(invocation -> {
            DispatchRecord record = invocation.getArgument(0);
            record.setId(100L);
            return 1;
        });

        DispatchRequest request = new DispatchRequest();
        request.setTaskId(1L);
        request.setAssigneeId(3L);

        DispatchResponse response = dispatchService.dispatch(request, 2L);

        assertEquals(100L, response.getDispatchId());
        assertEquals(1, response.getWorkOrderStatus());
        verify(workOrderStateMachine).transit(WorkOrderStatus.ISSUED, WorkOrderEvent.DISPATCH);
        verify(cacheService).deleteKey(com.fzy.mes.module.cache.RedisCacheKeys.WORK_ORDER_STATUS_STATS);
    }

    @Test
    void dispatchShouldNotTransitWhenWorkOrderAlreadyAssigned() {
        when(dispatchWorkerMapper.existsActiveWorker(3L)).thenReturn(true);

        OperationTask task = buildTask(2L, 10L, 0, null, 0);
        WorkOrder workOrder = buildWorkOrder(10L, 1, 1);
        when(operationTaskMapper.selectById(2L)).thenReturn(task);
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder);
        when(operationTaskMapper.update(isNull(), any())).thenReturn(1);
        when(dispatchRecordMapper.insert(any(DispatchRecord.class))).thenAnswer(invocation -> {
            DispatchRecord record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        });

        DispatchRequest request = new DispatchRequest();
        request.setTaskId(2L);
        request.setAssigneeId(3L);

        DispatchResponse response = dispatchService.dispatch(request, 2L);

        assertEquals(1, response.getWorkOrderStatus());
        verify(workOrderStateMachine, never()).transit(any(), any());
        verify(workOrderMapper, never()).update(isNull(), any());
        verify(cacheService, never()).deleteKey(any());
    }

    @Test
    void dispatchShouldRejectAlreadyAssignedTask() {
        when(dispatchWorkerMapper.existsActiveWorker(3L)).thenReturn(true);

        OperationTask task = buildTask(1L, 10L, 0, 5L, 0);
        when(operationTaskMapper.selectById(1L)).thenReturn(task);

        DispatchRequest request = new DispatchRequest();
        request.setTaskId(1L);
        request.setAssigneeId(3L);

        assertThrows(BusinessException.class, () -> dispatchService.dispatch(request, 2L));
        verify(dispatchRecordMapper, never()).insert(any(DispatchRecord.class));
    }

    private OperationTask buildTask(Long id, Long workOrderId, int version, Long assignedTo, int status) {
        OperationTask task = new OperationTask();
        task.setId(id);
        task.setWorkOrderId(workOrderId);
        task.setVersion(version);
        task.setAssignedTo(assignedTo);
        task.setStatus(status);
        return task;
    }

    private WorkOrder buildWorkOrder(Long id, int status, int version) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setStatus(status);
        workOrder.setVersion(version);
        return workOrder;
    }

}
