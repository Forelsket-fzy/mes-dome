package com.fzy.mes.module.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    @Select("""
    SELECT status, COUNT(*) AS count
    FROM work_order
    GROUP BY status
    """)
    List<Map<String, Object>> countGroupByStatus();

}
