package com.fzy.mes.module.erp.service;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.OperationRequest;
import com.fzy.mes.module.erp.service.impl.ErpWorkOrderServiceImpl;
import com.fzy.mes.module.erp.support.ErpOrderIdempotentStore;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpWorkOrderPushTest {

    private static final String ERP_ORDER_NO = "ERP-PUSH-001";

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
    void pushNewOrderShouldCreateWorkOrderAndTasks() {
        when(workOrderMapper.selectOne(any())).thenReturn(null);
        when(idempotentStore.tryMark(ERP_ORDER_NO)).thenReturn(true);
        when(workOrderMapper.insert(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder workOrder = invocation.getArgument(0);
            workOrder.setId(100L);
            return 1;
        });

        var response = erpWorkOrderService.push(sampleRequest());

        assertEquals(100L, response.getId());
        assertFalse(response.isDuplicated());
        verify(operationTaskMapper, times(2)).insert(any(OperationTask.class));
        verify(idempotentStore).bindWorkOrderId(ERP_ORDER_NO, 100L);
        verify(cacheService).deleteKey(RedisCacheKeys.WORK_ORDER_STATUS_STATS);

        ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderMapper).insert(captor.capture());
        WorkOrder saved = captor.getValue();
        assertEquals(ERP_ORDER_NO, saved.getErpOrderNo());
        assertEquals(0, saved.getStatus());
        assertEquals("RELEASED", saved.getErpStatus());
        assertEquals(10, saved.getPlanQty());
    }

    @Test
    void pushDuplicateShouldReturnExistingWithoutInsert() {
        WorkOrder existing = new WorkOrder();
        existing.setId(88L);
        existing.setErpOrderNo(ERP_ORDER_NO);
        when(workOrderMapper.selectOne(any())).thenReturn(existing);

        var response = erpWorkOrderService.push(sampleRequest());

        assertEquals(88L, response.getId());
        assertTrue(response.isDuplicated());
        verify(workOrderMapper, never()).insert(any(WorkOrder.class));
        verify(operationTaskMapper, never()).insert(any(OperationTask.class));
        verify(cacheService, never()).deleteKey(any());
    }

    @Test
    void pushWhenIdempotentBlockedAndNoRecordShouldThrow() {
        when(workOrderMapper.selectOne(any())).thenReturn(null);
        when(idempotentStore.tryMark(ERP_ORDER_NO)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> erpWorkOrderService.push(sampleRequest()));
        assertTrue(ex.getMessage().contains("推单处理中"));
    }

    private static ErpPushWorkOrderRequest sampleRequest() {
        ErpPushWorkOrderRequest request = new ErpPushWorkOrderRequest();
        request.setErpOrderNo(ERP_ORDER_NO);
        request.setProductCode("P-001");
        request.setProductName("演示产品");
        request.setPlanQty(10);

        OperationRequest op1 = new OperationRequest();
        op1.setSeq(1);
        op1.setOperationCode("OP10");
        op1.setOperationName("下料");

        OperationRequest op2 = new OperationRequest();
        op2.setSeq(2);
        op2.setOperationCode("OP20");
        op2.setOperationName("装配");

        request.setOperations(List.of(op1, op2));
        return request;
    }

}
