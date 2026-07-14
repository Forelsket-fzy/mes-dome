package com.fzy.mes.module.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.dispatch.entity.DispatchAuditLog;
import com.fzy.mes.module.dispatch.vo.AuditResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DispatchAuditLogMapper extends BaseMapper<DispatchAuditLog> {

    Page<AuditResponse> selectAuditPage(Page<AuditResponse> page,
                                        @Param("taskId") Long taskId,
                                        @Param("assigneeId") Long assigneeId,
                                        @Param("operatorId") Long operatorId);

}
