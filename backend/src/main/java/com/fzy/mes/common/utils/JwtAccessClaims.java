package com.fzy.mes.common.utils;

/**
 * 解析 access token 后的有效载荷。
 */
public record JwtAccessClaims(String username, String jti) {
}
