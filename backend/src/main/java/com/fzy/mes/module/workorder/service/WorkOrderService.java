package com.fzy.mes.module.workorder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.workorder.dto.WorkOrderQuery;
import com.fzy.mes.module.workorder.vo.WorkOrderDetailVO;
import com.fzy.mes.module.workorder.vo.WorkOrderListItemVO;
import com.fzy.mes.module.workorder.vo.WorkOrderStatusStatsItemsVO;

public interface WorkOrderService {

    Page<WorkOrderListItemVO> pageList(WorkOrderQuery query);

    WorkOrderDetailVO getDetail(Long id);

    WorkOrderStatusStatsItemsVO getStats();
}
