package com.fzy.mes.module.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.entity.SysUser;
import com.fzy.mes.module.auth.entity.UserAuth;
import com.fzy.mes.module.auth.mapper.UserAuthMapper;
import com.fzy.mes.module.auth.service.AuthService;
import com.fzy.mes.module.auth.vo.LoginUser;
import com.fzy.mes.module.cache.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl  implements UserDetailsService {

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private AuthService authService;

    @Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {

        //用户存在 从缓存中拿数据 不存在会直接抛异常
        AuthSession user = authService.getByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        List<GrantedAuthority> authorityList = List.of(new SimpleGrantedAuthority(user.getRole()));

        return new LoginUser(user.getId(), username, user.getPassword(), user.getEnabled(), authorityList);
    }
}
