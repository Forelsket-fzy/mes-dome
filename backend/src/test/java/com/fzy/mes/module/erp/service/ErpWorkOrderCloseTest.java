package com.fzy.mes.module.erp.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.service.impl.ErpWorkOrderServiceImpl;
import com.fzy.mes.module.erp.support.ErpOrderIdempotentStore;
import com.fzy.mes.module.erp.vo.ErpCloseWorkOrderResponse;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpWorkOrderCloseTest {

    private static final String ERP_ORDER_NO = "ERP-D14-001";

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private OperationTaskMapper operationTaskMapper;
    @Mock
    private ErpOrderIdempotentStore idempotentStore;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ErpWorkOrderServiceImpl erpWorkOrderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(erpWorkOrderService, "workOrderStateMachine", new WorkOrderStateMachine());
    }

    @Test
    void normalCloseFromCompletedShouldMoveToClosed() {
        WorkOrder existing = workOrder(1L, WorkOrderStatus.COMPLETED.getCode(), 1);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(workOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.NORMAL);
        req.setErpStatus("CLOSED");

        ErpCloseWorkOrderResponse response = erpWorkOrderService.close(ERP_ORDER_NO, req);

        assertEquals(1L, response.getWorkOrderId());
        assertEquals(WorkOrderStatus.COMPLETED.getCode(), response.getPreviousStatus());
        assertEquals(WorkOrderStatus.CLOSED.getCode(), response.getCurrentStatus());
        assertFalse(response.isDuplicate());
        verify(workOrderMapper).update(isNull(), any(UpdateWrapper.class));
        verify(cacheService).deleteKey(RedisCacheKeys.WORK_ORDER_STATUS_STATS);
    }

    @Test
    void normalCloseWhenAlreadyClosedShouldBeDuplicate() {
        WorkOrder existing = workOrder(2L, WorkOrderStatus.CLOSED.getCode(), 2);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.NORMAL);
        req.setErpStatus("CLOSED");

        ErpCloseWorkOrderResponse response = erpWorkOrderService.close(ERP_ORDER_NO, req);

        assertTrue(response.isDuplicate());
        assertEquals(WorkOrderStatus.CLOSED.getCode(), response.getCurrentStatus());
        verify(workOrderMapper, never()).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void normalCloseWhenInProgressShouldThrowConflict() {
        WorkOrder existing = workOrder(3L, WorkOrderStatus.IN_PROGRESS.getCode(), 1);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.NORMAL);
        req.setErpStatus("CLOSED");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> erpWorkOrderService.close(ERP_ORDER_NO, req));
        assertTrue(ex.getMessage().contains("状态冲突"));
    }

    @Test
    void cancelFromIssuedShouldMoveToCancelled() {
        WorkOrder existing = workOrder(4L, WorkOrderStatus.ISSUED.getCode(), 1);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);
        when(workOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.CANCELLED);
        req.setErpStatus("CANCELLED");
        req.setCancelReason("ERP 取消订单");

        ErpCloseWorkOrderResponse response = erpWorkOrderService.close(ERP_ORDER_NO, req);

        assertEquals(WorkOrderStatus.ISSUED.getCode(), response.getPreviousStatus());
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), response.getCurrentStatus());
        assertFalse(response.isDuplicate());

        ArgumentCaptor<UpdateWrapper<WorkOrder>> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(workOrderMapper).update(isNull(), captor.capture());
    }

    @Test
    void cancelWithoutReasonShouldThrow() {
        WorkOrder existing = workOrder(5L, WorkOrderStatus.ISSUED.getCode(), 1);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.CANCELLED);
        req.setErpStatus("CANCELLED");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> erpWorkOrderService.close(ERP_ORDER_NO, req));
        assertTrue(ex.getMessage().contains("取消关单必须填写原因"));
    }

    private static WorkOrder workOrder(long id, int status, int version) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setErpOrderNo(ERP_ORDER_NO);
        workOrder.setStatus(status);
        workOrder.setVersion(version);
        return workOrder;
    }

}
