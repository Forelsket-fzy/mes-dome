package com.fzy.mes.module.workorder.statemachine;

import com.fzy.mes.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkOrderStateMachineTest {

    private WorkOrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new WorkOrderStateMachine();
    }

    @Test
    void dispatchFromIssuedShouldBecomeAssigned() {
        WorkOrderStatus next = stateMachine.transit(WorkOrderStatus.ISSUED, WorkOrderEvent.DISPATCH);
        assertEquals(WorkOrderStatus.ASSIGNED, next);
    }

    @Test
    void skipDispatchShouldThrow() {
        assertThrows(BusinessException.class,
                () -> WorkOrderStateMachine.transition(WorkOrderStatus.ISSUED, WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void resolveAfterReportPartialShouldBecomePartialCompleted() {
        WorkOrderStatus next = stateMachine.resolveAfterReport(
                WorkOrderStatus.IN_PROGRESS, 100, 60, false);
        assertEquals(WorkOrderStatus.PARTIAL_COMPLETED, next);
    }

    @Test
    void resolveAfterReportLastOperationShouldBecomeCompleted() {
        WorkOrderStatus next = stateMachine.resolveAfterReport(
                WorkOrderStatus.IN_PROGRESS, 100, 100, true);
        assertEquals(WorkOrderStatus.COMPLETED, next);
    }

    @Test
    void finalStatusShouldNotTransit() {
        assertThrows(BusinessException.class,
                () -> stateMachine.transit(WorkOrderStatus.CLOSED, WorkOrderEvent.CLOSE));
        assertThrows(BusinessException.class,
                () -> stateMachine.resolveAfterReport(WorkOrderStatus.CANCELLED, 100, 50, false));
    }

    @Test
    void partialCompletedContinueReportShouldBecomeInProgress() {
        WorkOrderStatus next = stateMachine.transit(
                WorkOrderStatus.PARTIAL_COMPLETED, WorkOrderEvent.CONTINUE_REPORT);
        assertEquals(WorkOrderStatus.IN_PROGRESS, next);
    }

    @Test
    void fromCodeShouldMapDatabaseValue() {
        assertEquals(WorkOrderStatus.ISSUED, WorkOrderStatus.fromCode(0));
        assertEquals(WorkOrderStatus.CANCELLED, WorkOrderStatus.fromCode(6));
    }

}
