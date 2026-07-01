package com.fzy.mes.module.auth.service.impl;

import com.fzy.mes.common.utils.JwtAccessToken;
import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.config.AccessTokenBlacklist;
import com.fzy.mes.module.auth.config.RefreshTokenStore;
import com.fzy.mes.module.auth.dto.LoginRequest;
import com.fzy.mes.module.auth.service.AuthService;
import com.fzy.mes.module.auth.vo.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${mes.jwt.refresh-expire}")
    private long refreshExpireMs;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RefreshTokenStore refreshTokenStore;
    @Autowired
    private AccessTokenBlacklist accessTokenBlacklist;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Map<String, String> login(LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        JwtAccessToken access = jwtUtil.generateAccessToken(loginUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken();
        refreshTokenStore.save(refreshToken, loginUser.getUsername(), refreshExpireMs);

        return Map.of(
                "accessToken", access.token(),
                "refreshToken", refreshToken,
                "expiresIn", String.valueOf(access.expiresInMs()));
    }

    @Override
    public Map<String, String> refresh(String refreshToken) {
        String username = refreshTokenStore.getUsername(refreshToken);
        if (username == null) {
            return null;
        }

        refreshTokenStore.delete(refreshToken);

        JwtAccessToken access = jwtUtil.generateAccessToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken();
        refreshTokenStore.save(newRefreshToken, username, refreshExpireMs);

        return Map.of(
                "accessToken", access.token(),
                "refreshToken", newRefreshToken,
                "expiresIn", String.valueOf(access.expiresInMs()));
    }

    @Override
    public boolean logout(String refreshToken, String accessToken) {
        if (!refreshTokenStore.exists(refreshToken)) {
            return false;
        }
        refreshTokenStore.delete(refreshToken);

        if (accessToken != null) {
            jwtUtil.verifyAccessToken(accessToken).ifPresent(claims ->
                    jwtUtil.resolveAccessTokenRemainingMs(accessToken)
                            .ifPresent(remaining -> accessTokenBlacklist.add(claims.jti(), remaining)));
        }

        return true;
    }

}
