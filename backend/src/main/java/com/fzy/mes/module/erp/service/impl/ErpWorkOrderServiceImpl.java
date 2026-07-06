package com.fzy.mes.module.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;
import com.fzy.mes.module.erp.dto.OperationRequest;
import com.fzy.mes.module.erp.service.ErpWorkOrderService;
import com.fzy.mes.module.erp.support.ErpOrderIdempotentStore;
import com.fzy.mes.module.erp.vo.ErpCloseWorkOrderResponse;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderEvent;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ErpWorkOrderServiceImpl implements ErpWorkOrderService {

    private static final int MES_STATUS_RELEASED = 0;
    private static final int TASK_STATUS_PENDING = 0;
    private static final String DEFAULT_ERP_STATUS = "RELEASED";

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private WorkOrderStateMachine workOrderStateMachine;
    @Autowired
    private OperationTaskMapper operationTaskMapper;
    @Autowired
    private ErpOrderIdempotentStore idempotentStore;

    @Override
    @Transactional
    public ErpPushWorkOrderResponse push(ErpPushWorkOrderRequest req) {
        WorkOrder existing = findByErpOrderNo(req.getErpOrderNo());
        if (existing != null) {
            return new ErpPushWorkOrderResponse(existing.getId(), true);
        }

        if (!idempotentStore.tryMark(req.getErpOrderNo())) {
            existing = findByErpOrderNo(req.getErpOrderNo());
            if (existing != null) {
                return new ErpPushWorkOrderResponse(existing.getId(), true);
            }
            throw new BusinessException("推单处理中，请稍后重试");
        }

        try {
            WorkOrder wo = buildWorkOrder(req);
            workOrderMapper.insert(wo);

            for (OperationRequest op : req.getOperations()) {
                operationTaskMapper.insert(buildTask(wo.getId(), req.getPlanQty(), op));
            }

            idempotentStore.bindWorkOrderId(req.getErpOrderNo(), wo.getId());
            return new ErpPushWorkOrderResponse(wo.getId(), false);
        } catch (DuplicateKeyException e) {
            existing = findByErpOrderNo(req.getErpOrderNo());
            if (existing != null) {
                return new ErpPushWorkOrderResponse(existing.getId(), true);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public ErpCloseWorkOrderResponse close(String erpOrderNo, ErpCloseWorkOrderRequest req) {
        WorkOrder existing = findByErpOrderNo(erpOrderNo);
        if (existing == null) {
            throw new BusinessException("订单不存在");
        }

        WorkOrderStatus current = WorkOrderStatus.fromCode(existing.getStatus());

        if (req.getCloseType() == ErpCloseWorkOrderRequest.CloseType.NORMAL) {
            return handleNormalClose(req, existing, current);
        }
        if (req.getCloseType() == ErpCloseWorkOrderRequest.CloseType.CANCELLED) {
            return handleCancelledClose(req, existing, current);
        }

        throw new BusinessException("关单类型无效");
    }

    private ErpCloseWorkOrderResponse handleNormalClose(ErpCloseWorkOrderRequest req,
                                                        WorkOrder existing,
                                                        WorkOrderStatus current) {
        if (current == WorkOrderStatus.CLOSED) {
            return duplicateResponse(existing.getId(), WorkOrderStatus.CLOSED);
        }
        if (current == WorkOrderStatus.CANCELLED) {
            throw new BusinessException("工单已取消，无法执行正常关单");
        }
        if (current != WorkOrderStatus.COMPLETED) {
            throw new BusinessException("ERP正常关单但MES仍在生产中，状态冲突");
        }

        WorkOrderStatus next = workOrderStateMachine.transit(current, WorkOrderEvent.CLOSE);
        return persistClose(req, existing, current, next, false);
    }

    private ErpCloseWorkOrderResponse handleCancelledClose(ErpCloseWorkOrderRequest req,
                                                           WorkOrder existing,
                                                           WorkOrderStatus current) {
        if (current == WorkOrderStatus.CANCELLED) {
            return duplicateResponse(existing.getId(), WorkOrderStatus.CANCELLED);
        }
        if (current == WorkOrderStatus.COMPLETED || current == WorkOrderStatus.CLOSED) {
            throw new BusinessException("MES已完工或已关闭，无法取消");
        }
        if (!StringUtils.hasText(req.getCancelReason())) {
            throw new BusinessException("取消关单必须填写原因");
        }

        WorkOrderStatus next = workOrderStateMachine.transit(current, WorkOrderEvent.CANCEL);
        return persistClose(req, existing, current, next, true);
    }

    private ErpCloseWorkOrderResponse persistClose(ErpCloseWorkOrderRequest req,
                                                   WorkOrder existing,
                                                   WorkOrderStatus current,
                                                   WorkOrderStatus next,
                                                   boolean writeCancelReason) {
        UpdateWrapper<WorkOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", next.getCode())
                .set("erp_status", req.getErpStatus())
                .eq("id", existing.getId())
                .eq("version", existing.getVersion());

        if (writeCancelReason) {
            updateWrapper.set("cancel_reason", req.getCancelReason());
        }

        int updated = workOrderMapper.update(null, updateWrapper);
        if (updated != 1) {
            throw new BusinessException("并发更新失败");
        }

        return new ErpCloseWorkOrderResponse(
                existing.getId(), current.getCode(), next.getCode(), false, false);
    }

    private ErpCloseWorkOrderResponse duplicateResponse(long workOrderId, WorkOrderStatus status) {
        int code = status.getCode();
        return new ErpCloseWorkOrderResponse(workOrderId, code, code, true, false);
    }

    private WorkOrder findByErpOrderNo(String erpOrderNo) {
        return workOrderMapper.selectOne(
                Wrappers.<WorkOrder>lambdaQuery().eq(WorkOrder::getErpOrderNo, erpOrderNo));
    }

    private OperationTask buildTask(Long workOrderId, Integer planQty, OperationRequest op) {
        OperationTask task = new OperationTask();
        task.setWorkOrderId(workOrderId);
        task.setPlanQty(planQty);
        task.setCompletedQty(0);
        task.setSeq(op.getSeq());
        task.setPriority(op.getPriority() != null ? op.getPriority() : 0);
        task.setStatus(TASK_STATUS_PENDING);
        task.setOperationName(op.getOperationName());
        task.setOperationCode(op.getOperationCode());
        task.setPlannedStart(op.getPlannedStart());
        return task;
    }

    private WorkOrder buildWorkOrder(ErpPushWorkOrderRequest req) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setErpOrderNo(req.getErpOrderNo());
        workOrder.setErpStatus(req.getErpStatus() != null ? req.getErpStatus() : DEFAULT_ERP_STATUS);
        workOrder.setStatus(MES_STATUS_RELEASED);
        workOrder.setProductCode(req.getProductCode());
        workOrder.setProductName(req.getProductName());
        workOrder.setPlanQty(req.getPlanQty());
        workOrder.setCompletedQty(0);
        workOrder.setDueDate(req.getDueDate());
        return workOrder;
    }

}
