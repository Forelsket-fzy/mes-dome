package com.fzy.mes.module.workorder.statemachine;

public enum WorkOrderEvent {

    DISPATCH,

    FIRST_REPORT,

    REPORT_PARTIAL,

    REPORT_COMPLETE,

    CONTINUE_REPORT,

    CLOSE,

    CANCEL

}
