package com.fzy.mes.module.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.module.auth.entity.UserAuth;
import com.fzy.mes.module.auth.mapper.UserAuthMapper;
import com.fzy.mes.module.auth.vo.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl  implements UserDetailsService {

    @Autowired
    private UserAuthMapper userAuthMapper;


    @Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {

        //查询用户
        UserAuth user = userAuthMapper.selectOne(Wrappers.<UserAuth>lambdaQuery().eq(UserAuth::getUsername,username));

        if(user == null){
            throw new UsernameNotFoundException("用户不存在");
        }

        //将用户信息放入缓存中

        //将用户信息权限放入缓存中

        //返回用户权限信息
        List<GrantedAuthority> authorityList = null;

        boolean enabled = Integer.valueOf(1).equals(user.getEnabled());

        return new LoginUser(user.getUserId(), username, user.getPassword(), enabled, authorityList);
    }
}
