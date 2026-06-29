package com.fzy.mes.module.auth.service.impl;

import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.dto.LoginRequest;
import com.fzy.mes.module.auth.service.AuthService;
import com.fzy.mes.module.auth.vo.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

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
}
