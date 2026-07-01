package com.fzy.mes.common.utils;

/**
 * 登录签发 access token 的返回值。
 */
public record JwtAccessToken(String token, String jti, long expiresInMs) {
}
