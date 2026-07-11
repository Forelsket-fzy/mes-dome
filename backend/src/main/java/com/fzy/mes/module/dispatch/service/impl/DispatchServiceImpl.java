package com.fzy.mes.module.dispatch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.dispatch.dto.WorkerQuery;
import com.fzy.mes.module.dispatch.mapper.DispatchWorkerMapper;
import com.fzy.mes.module.dispatch.service.DispatchService;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    private DispatchWorkerMapper dispatchWorkerMapper;

    @Override
    public Page<WorkerListItemVO> pageWorkers(WorkerQuery query) {
        Page<WorkerListItemVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        return dispatchWorkerMapper.selectWorkerPage(page, query.getSkillLevel(), keyword);
    }

}
