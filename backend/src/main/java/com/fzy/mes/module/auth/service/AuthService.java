package com.fzy.mes.module.auth.service;

import com.fzy.mes.module.auth.dto.LoginRequest;

import java.util.Map;

public interface AuthService {
    Map<String,String> login(LoginRequest  req);
}
