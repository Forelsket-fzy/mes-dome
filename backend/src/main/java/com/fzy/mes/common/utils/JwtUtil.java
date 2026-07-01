package com.fzy.mes.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Access Token：JWT（type=access，含 jti、username）。
 * Refresh Token：随机 UUID，由调用方存入 Redis（见 RefreshTokenStore，手写）。
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String ISSUER = "mes-dome";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";

    @Value("${mes.jwt.secret}")
    private String secret;

    @Value("${mes.jwt.access-expire}")
    private long accessExpireMs;

    /**
     * 签发 access token（仅用于 API 鉴权）。
     */
    public JwtAccessToken generateAccessToken(String username) {
        String jti = newJti();
        Date now = new Date();
        String token = JWT.create()
                .withJWTId(jti)
                .withIssuer(ISSUER)
                .withSubject(username)
                .withClaim(CLAIM_USERNAME, username)
                .withClaim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + accessExpireMs))
                .sign(algorithm());
        return new JwtAccessToken(token, jti, accessExpireMs);
    }

    /**
     * 签发 refresh token（随机 UUID）。
     */
    public String generateRefreshToken() {
        return newJti();
    }

    /**
     * 校验 access token：签名、过期、issuer、type=access。
     */
    public Optional<JwtAccessClaims> verifyAccessToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm())
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            String type = jwt.getClaim(CLAIM_TYPE).asString();
            if (!TOKEN_TYPE_ACCESS.equals(type)) {
                log.debug("拒绝非 access 类型 token, type={}", type);
                return Optional.empty();
            }

            String username = jwt.getClaim(CLAIM_USERNAME).asString();
            String jti = jwt.getId();
            if (username == null || username.isBlank() || jti == null || jti.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new JwtAccessClaims(username, jti));
        } catch (JWTVerificationException ex) {
            log.debug("Access token 校验失败: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 校验 access token 并返回剩余有效毫秒数；无效或已过期返回 empty。
     */
    public Optional<Long> resolveAccessTokenRemainingMs(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm())
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            String type = jwt.getClaim(CLAIM_TYPE).asString();
            if (!TOKEN_TYPE_ACCESS.equals(type)) {
                return Optional.empty();
            }

            long remaining = jwt.getExpiresAt().getTime() - System.currentTimeMillis();
            return remaining > 0 ? Optional.of(remaining) : Optional.empty();
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

    /**
     * 从 Authorization 头提取 Bearer token；格式不对返回 null。
     */
    public static String resolveBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }


    private Algorithm algorithm() {
        return Algorithm.HMAC256(secret);
    }

    private static String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
