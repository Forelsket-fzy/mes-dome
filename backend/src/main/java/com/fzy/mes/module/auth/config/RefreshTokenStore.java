package com.fzy.mes.module.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "mes:auth:refresh:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void save(String refreshToken, String username, long expireMs) {
        redisTemplate.opsForValue().set(key(refreshToken), username, expireMs, TimeUnit.MILLISECONDS);
    }

    public boolean exists(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(refreshToken)));
    }

    public String getUsername(String refreshToken) {
        Object value = redisTemplate.opsForValue().get(key(refreshToken));
        return value == null ? null : value.toString();
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(key(refreshToken));
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + refreshToken;
    }

}
