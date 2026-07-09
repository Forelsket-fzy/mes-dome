package com.fzy.mes.module.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.workorder.dto.WorkOrderQuery;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.service.impl.WorkOrderServiceImpl;
import com.fzy.mes.module.workorder.vo.WorkOrderDetailVO;
import com.fzy.mes.module.workorder.vo.WorkOrderListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceQueryTest {

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private OperationTaskMapper operationTaskMapper;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    @Test
    void pageListShouldMapRecordsToVo() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setErpOrderNo("ERP-001");
        workOrder.setProductCode("P-001");
        workOrder.setStatus(0);
        workOrder.setPlanQty(5);

        Page<WorkOrder> entityPage = new Page<>(1, 10);
        entityPage.setRecords(List.of(workOrder));
        entityPage.setTotal(1);
        when(workOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(entityPage);

        WorkOrderQuery query = new WorkOrderQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setStatus(0);

        Page<WorkOrderListItemVO> result = workOrderService.pageList(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("ERP-001", result.getRecords().get(0).getErpOrderNo());
    }

    @Test
    void getDetailShouldIncludeOperationsOrderedBySeq() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(10L);
        workOrder.setErpOrderNo("ERP-010");
        workOrder.setProductName("演示产品");
        workOrder.setStatus(0);
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder);

        OperationTask task1 = new OperationTask();
        task1.setSeq(1);
        task1.setOperationCode("OP10");
        task1.setOperationName("下料");

        OperationTask task2 = new OperationTask();
        task2.setSeq(2);
        task2.setOperationCode("OP20");
        task2.setOperationName("装配");
        when(operationTaskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(task1, task2));

        WorkOrderDetailVO detail = workOrderService.getDetail(10L);

        assertEquals("ERP-010", detail.getErpOrderNo());
        assertEquals(2, detail.getOperations().size());
        assertEquals("OP10", detail.getOperations().get(0).getOperationCode());
        assertEquals("OP20", detail.getOperations().get(1).getOperationCode());
        verify(operationTaskMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void getDetailWhenNotFoundShouldThrow() {
        when(workOrderMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.getDetail(999L));
        assertEquals("工单不存在", ex.getMessage());
    }

}
