package com.fzy.mes.module.auth.service;

import com.fzy.mes.module.auth.dto.LoginRequest;

import java.util.Map;

public interface AuthService {

    Map<String, String> login(LoginRequest req);

    Map<String, String> refresh(String refreshToken);

    boolean logout(String refreshToken, String accessToken);

}
