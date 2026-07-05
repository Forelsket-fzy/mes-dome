package com.fzy.mes.module.workorder.statemachine;

import com.fzy.mes.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class WorkOrderStateMachine {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(WorkOrderStatus.class);

    static {
        // 已下发 → 可以派工 或 取消
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.ISSUED,
                Set.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED));

        // 已派工 → 可以开始执行 或 已完成 部分完成 取消
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.ASSIGNED,
                Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.PARTIAL_COMPLETED,
                        WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED));

        // 执行中 → 可以执行中,部分完工、直接完工、取消
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.IN_PROGRESS,
                Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.PARTIAL_COMPLETED,
                        WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED));

        // ★ 部分完工 ：可以执行中,继续报工、彻底完工、取消
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.PARTIAL_COMPLETED,
                Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.PARTIAL_COMPLETED,
                        WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED));

        // 已完工 → 只能关闭（归档）
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.COMPLETED,
                Set.of(WorkOrderStatus.CLOSED));

        // 已取消,已关闭 → 终态，哪儿也去不了
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.CANCELLED, Collections.emptySet());
        ALLOWED_TRANSITIONS.put(WorkOrderStatus.CLOSED, Collections.emptySet());
    }

    public WorkOrderStatus transit(WorkOrderStatus current, WorkOrderEvent event) {
        if (current.isFinal()) {
            throw new BusinessException("终态不可操作: " + current);
        }
        WorkOrderStatus next = resolveNext(current, event);
        transition(current, next);
        return next;
    }

    public WorkOrderStatus resolveAfterReport(WorkOrderStatus current, int planQty, int completedQty,
                                              boolean lastOperationDone) {
        if (current.isFinal() || current == WorkOrderStatus.CLOSED) {
            throw new BusinessException("终态不可报工: " + current);
        }
        if (current == WorkOrderStatus.COMPLETED) {
            throw new BusinessException("已完工不可再报工");
        }
        if (planQty <= 0) {
            throw new BusinessException("计划数量无效");
        }
        if (completedQty < 0 || completedQty > planQty) {
            throw new BusinessException("完工数量异常");
        }

        WorkOrderStatus target;
        if (lastOperationDone && completedQty >= planQty) {
            target = WorkOrderStatus.COMPLETED;
        } else if (completedQty > 0 && completedQty < planQty) {
            target = WorkOrderStatus.PARTIAL_COMPLETED;
        } else {
            target = WorkOrderStatus.IN_PROGRESS;
        }

        transition(current, target);
        return target;
    }

    public static void transition(WorkOrderStatus from, WorkOrderStatus to) {
        if (from == to) {
            return;
        }
        if (from.isFinal()) {
            throw new BusinessException("终态不可跃迁: " + from);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to)) {
            throw new BusinessException("非法状态跃迁: " + from + " -> " + to);
        }
    }

    private WorkOrderStatus resolveNext(WorkOrderStatus current, WorkOrderEvent event) {
        return switch (event) {
            case DISPATCH -> WorkOrderStatus.ASSIGNED;
            case FIRST_REPORT, CONTINUE_REPORT -> WorkOrderStatus.IN_PROGRESS;
            case REPORT_PARTIAL -> WorkOrderStatus.PARTIAL_COMPLETED;
            case REPORT_COMPLETE -> WorkOrderStatus.COMPLETED;
            case CLOSE -> WorkOrderStatus.CLOSED;
            case CANCEL -> WorkOrderStatus.CANCELLED;
        };
    }

}
