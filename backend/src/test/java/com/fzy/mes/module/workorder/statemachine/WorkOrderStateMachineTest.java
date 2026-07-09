package com.fzy.mes.module.workorder.statemachine;

import com.fzy.mes.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkOrderStateMachineTest {

    private WorkOrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new WorkOrderStateMachine();
    }

    @Test
    void dispatchShouldMoveIssuedToAssigned() {
        WorkOrderStatus next = stateMachine.transit(WorkOrderStatus.ISSUED, WorkOrderEvent.DISPATCH);
        assertEquals(WorkOrderStatus.ASSIGNED, next);
    }

    @Test
    void cancelShouldMoveIssuedToCancelled() {
        WorkOrderStatus next = stateMachine.transit(WorkOrderStatus.ISSUED, WorkOrderEvent.CANCEL);
        assertEquals(WorkOrderStatus.CANCELLED, next);
    }

    @Test
    void closeShouldMoveCompletedToClosed() {
        WorkOrderStatus next = stateMachine.transit(WorkOrderStatus.COMPLETED, WorkOrderEvent.CLOSE);
        assertEquals(WorkOrderStatus.CLOSED, next);
    }

    @Test
    void illegalTransitionShouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> stateMachine.transit(WorkOrderStatus.ISSUED, WorkOrderEvent.REPORT_COMPLETE));
        assertTrue(ex.getMessage().contains("非法状态跃迁"));
    }

    @Test
    void finalStateShouldRejectTransit() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> stateMachine.transit(WorkOrderStatus.CLOSED, WorkOrderEvent.DISPATCH));
        assertTrue(ex.getMessage().contains("终态不可操作"));
    }

    @Test
    void resolveAfterReportShouldReturnPartialCompleted() {
        WorkOrderStatus next = stateMachine.resolveAfterReport(
                WorkOrderStatus.IN_PROGRESS, 100, 40, false);
        assertEquals(WorkOrderStatus.PARTIAL_COMPLETED, next);
    }

    @Test
    void resolveAfterReportShouldReturnCompletedWhenLastOperationDone() {
        WorkOrderStatus next = stateMachine.resolveAfterReport(
                WorkOrderStatus.IN_PROGRESS, 100, 100, true);
        assertEquals(WorkOrderStatus.COMPLETED, next);
    }

}
