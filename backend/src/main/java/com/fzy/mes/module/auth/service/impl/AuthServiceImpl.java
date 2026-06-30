package com.fzy.mes.module.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.dto.LoginRequest;
import com.fzy.mes.module.auth.entity.SysUser;
import com.fzy.mes.module.auth.entity.UserAuth;
import com.fzy.mes.module.auth.mapper.UserAuthMapper;
import com.fzy.mes.module.auth.service.AuthService;
import com.fzy.mes.module.auth.vo.LoginUser;
import com.fzy.mes.module.cache.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserAuthMapper userAuthMapper;

    @Override
    public Map<String,String> login(LoginRequest req) {

        //AuthenticationManager 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(),req.getPassword()));

        //将认证信息存入SecurityContext中
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // authenticate 成功时 principal 必为 LoginUser
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = JwtUtil.generateToken(loginUser.getUsername());
        return Map.of("token", token);
    }

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
            cacheService.setValue("mes:user:" + username, user);
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

    @Override
    public SysUser findUserByUserID(Long userId) {
        //根据用户名查询SysUser数据
        return userAuthMapper.findUserByUserId(userId);
    }
}
