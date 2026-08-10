package com.fzy.mes.module.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fzy.mes.module.dispatch.vo.MyTaskItemVO;
import com.fzy.mes.module.workorder.entity.OperationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationTaskMapper extends BaseMapper<OperationTask> {

	List<MyTaskItemVO> selectMyTasks(@Param("workerId") Long workerId);

}
