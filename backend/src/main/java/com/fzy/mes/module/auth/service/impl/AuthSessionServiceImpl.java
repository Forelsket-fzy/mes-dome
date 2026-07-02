package com.fzy.mes.module.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.entity.SysUser;
import com.fzy.mes.module.auth.entity.UserAuth;
import com.fzy.mes.module.auth.mapper.UserAuthMapper;
import com.fzy.mes.module.auth.service.AuthSessionService;
import com.fzy.mes.module.cache.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private UserAuthMapper userAuthMapper;

    @Override
    public AuthSession loadFromDb(String username) {
        UserAuth authUser = userAuthMapper.selectOne(
                Wrappers.<UserAuth>lambdaQuery().eq(UserAuth::getUsername, username));
        if (authUser == null) {
            return null;
        }

        SysUser sysUser = userAuthMapper.findUserByUserId(authUser.getUserId());
        if (sysUser == null) {
            log.warn("sys_user 不存在, userId={}", authUser.getUserId());
            return null;
        }

        String role = userAuthMapper.findRoleByUserID(authUser.getUserId());
        AuthSession user = new AuthSession();
        user.setUsername(username);
        user.setPassword(authUser.getPassword());
        user.setId(sysUser.getId());
        user.setPhone(sysUser.getPhone());
        user.setRealName(sysUser.getRealName());
        user.setEnabled(Integer.valueOf(1).equals(authUser.getEnabled()));
        user.setRole(role);
        user.setSkillLevel(sysUser.getSkillLevel());

        try {
            cacheService.setValueWithExpire("mes:user:" + username, user, 3, TimeUnit.HOURS);
        } catch (RuntimeException e) {
            log.error("写入 Redis 失败, username={}", username, e);
        }

        return user;
    }

    @Override
    public AuthSession getByUsername(String username) {
        Object cached = cacheService.getValue("mes:user:" + username);
        AuthSession user = null;
        if (cached instanceof AuthSession authSession) {
            user = authSession;
        } else if (cached != null) {
            log.warn("Redis 会话类型不匹配, username={}, 回源查库", username);
        }

        if (user == null) {
            user = loadFromDb(username);
        }

        return user;
    }

}
