package com.fzy.mes.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.workorder.dto.WorkOrderQuery;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.service.WorkOrderService;
import com.fzy.mes.module.workorder.vo.OperationTaskVO;
import com.fzy.mes.module.workorder.vo.WorkOrderDetailVO;
import com.fzy.mes.module.workorder.vo.WorkOrderListItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private OperationTaskMapper operationTaskMapper;

    @Override
    public Page<WorkOrderListItemVO> pageList(WorkOrderQuery query) {
        Page<WorkOrder> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<WorkOrder> wrapper = Wrappers.<WorkOrder>lambdaQuery()
                .eq(query.getStatus() != null, WorkOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getErpOrderNo()), WorkOrder::getErpOrderNo, query.getErpOrderNo())
                .like(StringUtils.hasText(query.getProductCode()), WorkOrder::getProductCode, query.getProductCode())
                .orderByDesc(WorkOrder::getId);

        Page<WorkOrder> result = workOrderMapper.selectPage(page, wrapper);

        Page<WorkOrderListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toListItemVO).toList());
        return voPage;
    }

    @Override
    public WorkOrderDetailVO getDetail(Long id) {
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BusinessException("工单不存在");
        }

        List<OperationTask> tasks = operationTaskMapper.selectList(
                Wrappers.<OperationTask>lambdaQuery()
                        .eq(OperationTask::getWorkOrderId, workOrder.getId())
                        .orderByAsc(OperationTask::getSeq));

        WorkOrderDetailVO detail = toDetailVO(workOrder);
        detail.setOperations(tasks.stream().map(this::toOperationTaskVO).toList());
        return detail;
    }

    private WorkOrderListItemVO toListItemVO(WorkOrder workOrder) {
        WorkOrderListItemVO vo = new WorkOrderListItemVO();
        BeanUtils.copyProperties(workOrder, vo);
        return vo;
    }

    private WorkOrderDetailVO toDetailVO(WorkOrder workOrder) {
        WorkOrderDetailVO vo = new WorkOrderDetailVO();
        BeanUtils.copyProperties(workOrder, vo);
        return vo;
    }

    private OperationTaskVO toOperationTaskVO(OperationTask task) {
        OperationTaskVO vo = new OperationTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

}
