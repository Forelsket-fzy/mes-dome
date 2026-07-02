package com.fzy.mes.module.erp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;
import com.fzy.mes.module.erp.dto.OperationDto;
import com.fzy.mes.module.erp.service.ErpWorkOrderService;
import com.fzy.mes.module.erp.support.ErpOrderIdempotentStore;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErpWorkOrderServiceImpl implements ErpWorkOrderService {

    private static final int MES_STATUS_RELEASED = 0;
    private static final int TASK_STATUS_PENDING = 0;
    private static final String DEFAULT_ERP_STATUS = "RELEASED";

    @Autowired
    private WorkOrderMapper workOrderMapper;
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


        //处理幂等
        if (!idempotentStore.tryMark(req.getErpOrderNo())) {
            existing = findByErpOrderNo(req.getErpOrderNo());
            if (existing != null) {
                return new ErpPushWorkOrderResponse(existing.getId(), true);
            }
            throw new BusinessException("推单处理中，请稍后重试");
        }

        //开始写入数据库
        try {
            WorkOrder wo = buildWorkOrder(req);
            workOrderMapper.insert(wo);

            for (OperationDto op : req.getOperations()) {
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

    private WorkOrder findByErpOrderNo(String erpOrderNo) {
        return workOrderMapper.selectOne(
                Wrappers.<WorkOrder>lambdaQuery().eq(WorkOrder::getErpOrderNo, erpOrderNo));
    }

    private OperationTask buildTask(Long workOrderId, Integer planQty, OperationDto op) {
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
