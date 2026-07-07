package com.fzy.mes.module.workorder.service;

import com.fzy.mes.module.workorder.service.impl.WorkOrderServiceImpl;
import com.fzy.mes.module.workorder.vo.WorkOrderStatusStatsItemsVO;
import com.fzy.mes.module.workorder.vo.WorkOrderStatusVO;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderStatsTest {

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private OperationTaskMapper operationTaskMapper;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    @Test
    void getStatsShouldReturnAllSevenStatuses() {
        ReflectionTestUtils.setField(workOrderService, "statsTtlSeconds", 60L);
        when(cacheService.getValue("mes:stats:wo:status")).thenReturn(null);
        when(workOrderMapper.countGroupByStatus()).thenReturn(List.of(
                row(0, 3),
                row(2, 2)
        ));

        WorkOrderStatusStatsItemsVO stats = workOrderService.getStats();

        assertEquals(7, stats.getItems().size());
        assertEquals(5, stats.getTotal());
        assertEquals(0, findCount(stats, 1));
        assertEquals(3, findCount(stats, 0));
        assertEquals(2, findCount(stats, 2));
        assertEquals("已下发", findLabel(stats, 0));
        verify(cacheService).setValueWithExpire(eq("mes:stats:wo:status"), eq(stats), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void getStatsShouldHitCacheOnSecondCall() {
        WorkOrderStatusStatsItemsVO cached = new WorkOrderStatusStatsItemsVO();
        cached.setTotal(10);
        when(cacheService.getValue("mes:stats:wo:status")).thenReturn(cached);

        WorkOrderStatusStatsItemsVO stats = workOrderService.getStats();

        assertSame(cached, stats);
        verify(workOrderMapper, times(0)).countGroupByStatus();
    }

    @Test
    void getStatsTotalShouldEqualSumOfCounts() {
        ReflectionTestUtils.setField(workOrderService, "statsTtlSeconds", 60L);
        when(cacheService.getValue("mes:stats:wo:status")).thenReturn(null);
        when(workOrderMapper.countGroupByStatus()).thenReturn(List.of(
                row(0, 1),
                row(1, 2),
                row(4, 3)
        ));

        WorkOrderStatusStatsItemsVO stats = workOrderService.getStats();

        long sum = stats.getItems().stream().mapToLong(WorkOrderStatusVO::getCount).sum();
        assertEquals(sum, stats.getTotal());
        assertEquals(6, stats.getTotal());
    }

    private static Map<String, Object> row(int status, long count) {
        Map<String, Object> row = new HashMap<>();
        row.put("status", status);
        row.put("count", count);
        return row;
    }

    private static long findCount(WorkOrderStatusStatsItemsVO stats, int status) {
        return stats.getItems().stream()
                .filter(item -> item.getStatus() == status)
                .findFirst()
                .orElseThrow()
                .getCount();
    }

    private static String findLabel(WorkOrderStatusStatsItemsVO stats, int status) {
        return stats.getItems().stream()
                .filter(item -> item.getStatus() == status)
                .findFirst()
                .orElseThrow()
                .getStatusLabel();
    }

}
