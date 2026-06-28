package com.fzy.mes.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fzy.mes.module.auth.entity.UserAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {

}
