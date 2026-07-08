package com.fzy.mes.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.workorder.dto.WorkOrderQuery;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.service.WorkOrderService;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import com.fzy.mes.module.workorder.vo.OperationTaskVO;
import com.fzy.mes.module.workorder.vo.WorkOrderDetailVO;
import com.fzy.mes.module.workorder.vo.WorkOrderListItemVO;
import com.fzy.mes.module.workorder.vo.WorkOrderStatusStatsItemsVO;
import com.fzy.mes.module.workorder.vo.WorkOrderStatusVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private OperationTaskMapper operationTaskMapper;
    @Autowired
    private CacheService cacheService;

    @Value("${mes.redis.cache.work-order-stats-ttl-seconds:60}")
    private long workOrderStatsTtlSeconds;

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

    @Override
    public WorkOrderStatusStatsItemsVO getStats() {
        WorkOrderStatusStatsItemsVO cached = cacheService.getValue(
                RedisCacheKeys.WORK_ORDER_STATUS_STATS, WorkOrderStatusStatsItemsVO.class);
        if (cached != null) {
            return cached;
        }

        WorkOrderStatusStatsItemsVO stats = buildStatsFromDb();
        if (workOrderStatsTtlSeconds > 0) {
            cacheService.setValueWithExpire(
                    RedisCacheKeys.WORK_ORDER_STATUS_STATS,
                    stats,
                    workOrderStatsTtlSeconds,
                    TimeUnit.SECONDS);
        }
        return stats;
    }

    private WorkOrderStatusStatsItemsVO buildStatsFromDb() {
        Map<Integer, Long> countByStatus = new HashMap<>();
        for (Map<String, Object> row : workOrderMapper.countGroupByStatus()) {
            int status = ((Number) row.get("status")).intValue();
            long count = ((Number) row.get("count")).longValue();
            countByStatus.put(status, count);
        }

        WorkOrderStatusStatsItemsVO vo = new WorkOrderStatusStatsItemsVO();
        long total = 0;
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            long count = countByStatus.getOrDefault(status.getCode(), 0L);
            total += count;

            WorkOrderStatusVO item = new WorkOrderStatusVO();
            item.setStatus(status.getCode());
            item.setStatusLabel(status.getName());
            item.setCount(count);
            vo.getItems().add(item);
        }
        vo.setTotal(total);
        return vo;
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
