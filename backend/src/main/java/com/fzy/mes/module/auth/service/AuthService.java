package com.fzy.mes.module.auth.service;

import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.dto.LoginRequest;
import com.fzy.mes.module.auth.entity.SysUser;

import java.util.Map;

public interface AuthService {
    Map<String,String> login(LoginRequest  req);

    AuthSession loadFromDb(String username);

    AuthSession getByUsername(String username);

    SysUser findUserByUserID(Long userID);
}
