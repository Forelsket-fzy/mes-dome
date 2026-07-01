package com.fzy.mes.module.auth.controller;

import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.dto.LoginRequest;
import com.fzy.mes.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/login")
    public Result<Map<String, String>> login(@RequestBody @Valid LoginRequest req) {
        return Result.success(authService.login(req));
    }

    /**
     * 使用 refresh token 换新 access token（Header: Authorization: Bearer {refreshToken}）
     */
    @PostMapping("/auth/refresh")
    public Result<Map<String, String>> refresh(@RequestHeader("Authorization") String authorization) {
        String refreshToken = JwtUtil.resolveBearerToken(authorization);
        if (refreshToken == null) {
            return Result.unauthorized("refresh token 不能为空");
        }

        Map<String, String> tokens = authService.refresh(refreshToken);
        if (tokens == null) {
            return Result.unauthorized("refresh token 无效或已过期");
        }
        return Result.success(tokens);
    }

    /**
     * 登出：吊销 refresh token。
     * Header Authorization: Bearer {refreshToken}
     * 可选 Header X-Access-Token: Bearer {accessToken}，将当前 access jti 入黑名单
     */
    @PostMapping("/auth/logout")
    public Result<Void> logout(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Access-Token", required = false) String accessAuthorization) {
        String refreshToken = JwtUtil.resolveBearerToken(authorization);
        if (refreshToken == null) {
            return Result.unauthorized("refresh token 不能为空");
        }
        String accessToken = JwtUtil.resolveBearerToken(accessAuthorization);
        if (!authService.logout(refreshToken, accessToken)) {
            return Result.unauthorized("refresh token 无效或已过期");
        }
        return Result.success();
    }

}
