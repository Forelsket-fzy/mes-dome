package com.fzy.mes.module.workorder.statemachine;

import com.fzy.mes.common.exception.BusinessException;
import lombok.Getter;

@Getter
public enum WorkOrderStatus {

    ISSUED(0, "已下发", StatusCategory.ACTIVE),
    ASSIGNED(1, "已派工", StatusCategory.ACTIVE),
    IN_PROGRESS(2, "执行中", StatusCategory.ACTIVE),
    PARTIAL_COMPLETED(3, "部分完工", StatusCategory.ACTIVE),
    COMPLETED(4, "已完工", StatusCategory.WAITING),
    CLOSED(5, "已关闭", StatusCategory.FINAL),
    CANCELLED(6, "已取消", StatusCategory.FINAL);

    private final int code;
    private final String name;
    private final StatusCategory category;

    WorkOrderStatus(int code, String name, StatusCategory category) {
        this.code = code;
        this.name = name;
        this.category = category;
    }

    public static WorkOrderStatus fromCode(int code) {
        for (WorkOrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new BusinessException("未知工单状态: " + code);
    }

    public boolean isActive() {
        return this.category == StatusCategory.ACTIVE;
    }

    public boolean isFinal() {
        return this.category == StatusCategory.FINAL;
    }

    public enum StatusCategory {
        ACTIVE,
        WAITING,
        FINAL,
    }

}
