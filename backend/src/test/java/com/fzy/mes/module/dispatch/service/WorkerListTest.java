package com.fzy.mes.module.dispatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.dispatch.dto.WorkerQuery;
import com.fzy.mes.module.dispatch.mapper.DispatchWorkerMapper;
import com.fzy.mes.module.dispatch.service.impl.DispatchServiceImpl;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerListTest {

    @Mock
    private DispatchWorkerMapper dispatchWorkerMapper;

    @InjectMocks
    private DispatchServiceImpl dispatchService;

    @Test
    void pageWorkersShouldReturnMappedPage() {
        WorkerListItemVO worker = new WorkerListItemVO();
        worker.setId(3L);
        worker.setUsername("worker1");
        worker.setRealName("张三");
        worker.setSkillLevel(4);

        Page<WorkerListItemVO> page = new Page<>(1, 20);
        page.setRecords(List.of(worker));
        page.setTotal(1);
        when(dispatchWorkerMapper.selectWorkerPage(org.mockito.ArgumentMatchers.any(), isNull(), isNull()))
                .thenReturn(page);

        WorkerQuery query = new WorkerQuery();
        Page<WorkerListItemVO> result = dispatchService.pageWorkers(query);

        assertEquals(1, result.getTotal());
        assertEquals("worker1", result.getRecords().get(0).getUsername());
        assertEquals(4, result.getRecords().get(0).getSkillLevel());
        verify(dispatchWorkerMapper).selectWorkerPage(org.mockito.ArgumentMatchers.any(), isNull(), isNull());
    }

    @Test
    void pageWorkersShouldPassSkillLevelFilter() {
        Page<WorkerListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(dispatchWorkerMapper.selectWorkerPage(org.mockito.ArgumentMatchers.any(), eq(5), isNull()))
                .thenReturn(page);

        WorkerQuery query = new WorkerQuery();
        query.setSkillLevel(5);
        dispatchService.pageWorkers(query);

        verify(dispatchWorkerMapper).selectWorkerPage(org.mockito.ArgumentMatchers.any(), eq(5), isNull());
    }

    @Test
    void pageWorkersShouldTrimKeyword() {
        Page<WorkerListItemVO> page = new Page<>(1, 20);
        page.setRecords(List.of());
        when(dispatchWorkerMapper.selectWorkerPage(org.mockito.ArgumentMatchers.any(), isNull(), eq("张")))
                .thenReturn(page);

        WorkerQuery query = new WorkerQuery();
        query.setKeyword(" 张 ");
        dispatchService.pageWorkers(query);

        verify(dispatchWorkerMapper).selectWorkerPage(org.mockito.ArgumentMatchers.any(), isNull(), eq("张"));
    }

}
