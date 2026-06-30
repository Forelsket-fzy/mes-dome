package com.fzy.mes.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fzy.mes.module.auth.entity.SysUser;
import com.fzy.mes.module.auth.entity.UserAuth;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {

    @Select("select * from sys_user where id = #{userId}")
    SysUser findUserByUserId(Long userId);

    @Select("select r.role_code from role r join user_role ur on r.id = ur.role_id" +
            " where ur.user_id = #{userId}")
    String findRoleByUserID( Long userId);
}
