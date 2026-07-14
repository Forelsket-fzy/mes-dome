package com.fzy.mes.module.dispatch.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.dispatch.vo.WorkerListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DispatchWorkerMapper {

    Page<WorkerListItemVO> selectWorkerPage(Page<WorkerListItemVO> page,
                                            @Param("skillLevel") Integer skillLevel,
                                            @Param("keyword") String keyword);

    boolean existsActiveWorker(@Param("userId") Long userId);

}
