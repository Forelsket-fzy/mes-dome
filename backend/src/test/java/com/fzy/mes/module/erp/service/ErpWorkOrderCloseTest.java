package com.fzy.mes.module.erp.service;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.service.impl.ErpWorkOrderServiceImpl;
import com.fzy.mes.module.erp.vo.ErpCloseWorkOrderResponse;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpWorkOrderCloseTest {

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private OperationTaskMapper operationTaskMapper;
    @Spy
    private WorkOrderStateMachine workOrderStateMachine = new WorkOrderStateMachine();

    @InjectMocks
    private ErpWorkOrderServiceImpl erpWorkOrderService;

    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(erpWorkOrderService, "workOrderStateMachine", workOrderStateMachine);
        workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setErpOrderNo("ERP-001");
        workOrder.setStatus(WorkOrderStatus.COMPLETED.getCode());
        workOrder.setVersion(1);
    }

    @Test
    void normalCloseFromCompletedShouldBecomeClosed() {
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(workOrderMapper.update(any(), any())).thenReturn(1);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.NORMAL);
        req.setErpStatus("CLOSED");

        ErpCloseWorkOrderResponse response = erpWorkOrderService.close("ERP-001", req);

        assertEquals(4, response.getPreviousStatus());
        assertEquals(5, response.getCurrentStatus());
        assertFalse(response.isDuplicate());
    }

    @Test
    void normalCloseWhileInProgressShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.NORMAL);

        assertThrows(BusinessException.class, () -> erpWorkOrderService.close("ERP-001", req));
        verify(workOrderMapper, never()).update(any(), any());
    }

    @Test
    void cancelFromIssuedShouldBecomeCancelled() {
        workOrder.setStatus(WorkOrderStatus.ISSUED.getCode());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(workOrderMapper.update(any(), any())).thenReturn(1);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.CANCELLED);
        req.setErpStatus("CANCELLED");
        req.setCancelReason("客户取消");

        ErpCloseWorkOrderResponse response = erpWorkOrderService.close("ERP-001", req);

        assertEquals(6, response.getCurrentStatus());
    }

    @Test
    void duplicateCancelledCloseShouldReturnDuplicate() {
        workOrder.setStatus(WorkOrderStatus.CANCELLED.getCode());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.CANCELLED);

        ErpCloseWorkOrderResponse response = erpWorkOrderService.close("ERP-001", req);

        assertTrue(response.isDuplicate());
        assertEquals(6, response.getPreviousStatus());
        assertEquals(6, response.getCurrentStatus());
        verify(workOrderMapper, never()).update(any(), any());
    }

    @Test
    void cancelCompletedWorkOrderShouldThrowConflict() {
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);

        ErpCloseWorkOrderRequest req = new ErpCloseWorkOrderRequest();
        req.setCloseType(ErpCloseWorkOrderRequest.CloseType.CANCELLED);
        req.setCancelReason("客户取消");

        assertThrows(BusinessException.class, () -> erpWorkOrderService.close("ERP-001", req));
    }

}
