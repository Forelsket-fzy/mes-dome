package com.fzy.mes.module.erp.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.OperationRequest;
import com.fzy.mes.module.erp.service.impl.ErpWorkOrderServiceImpl;
import com.fzy.mes.module.erp.support.ErpOrderIdempotentStore;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D14 推单全流程：推单 → 取消关单 → 幂等关单。
 */
@ExtendWith(MockitoExtension.class)
class ErpWorkOrderFlowTest {

    private static final String ERP_ORDER_NO = "ERP-FLOW-001";

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
    void pushThenCancelThenDuplicateCancel() {
        when(workOrderMapper.selectOne(any())).thenReturn(null, workOrder(200L, 0, 1), workOrder(200L, 6, 2));
        when(idempotentStore.tryMark(ERP_ORDER_NO)).thenReturn(true);
        when(workOrderMapper.insert(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder workOrder = invocation.getArgument(0);
            workOrder.setId(200L);
            return 1;
        });
        when(workOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        var pushResponse = erpWorkOrderService.push(buildPushRequest());
        assertEquals(200L, pushResponse.getId());
        assertFalse(pushResponse.isDuplicated());

        ErpCloseWorkOrderRequest cancelReq = new ErpCloseWorkOrderRequest();
        cancelReq.setCloseType(ErpCloseWorkOrderRequest.CloseType.CANCELLED);
        cancelReq.setErpStatus("CANCELLED");
        cancelReq.setCancelReason("客户撤单");

        var cancelResponse = erpWorkOrderService.close(ERP_ORDER_NO, cancelReq);
        assertEquals(WorkOrderStatus.ISSUED.getCode(), cancelResponse.getPreviousStatus());
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), cancelResponse.getCurrentStatus());
        assertFalse(cancelResponse.isDuplicate());

        var duplicateCancel = erpWorkOrderService.close(ERP_ORDER_NO, cancelReq);
        assertTrue(duplicateCancel.isDuplicate());
        assertEquals(WorkOrderStatus.CANCELLED.getCode(), duplicateCancel.getCurrentStatus());

        verify(cacheService, times(2)).deleteKey(RedisCacheKeys.WORK_ORDER_STATUS_STATS);
    }

    private static ErpPushWorkOrderRequest buildPushRequest() {
        ErpPushWorkOrderRequest request = new ErpPushWorkOrderRequest();
        request.setErpOrderNo(ERP_ORDER_NO);
        request.setProductCode("P-FLOW");
        request.setProductName("全流程演示");
        request.setPlanQty(20);

        OperationRequest op = new OperationRequest();
        op.setSeq(1);
        op.setOperationCode("OP10");
        op.setOperationName("加工");
        request.setOperations(List.of(op));
        return request;
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
