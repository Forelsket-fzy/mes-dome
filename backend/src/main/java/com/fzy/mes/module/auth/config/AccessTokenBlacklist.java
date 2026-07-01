package com.fzy.mes.module.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AccessTokenBlacklist {

    private static final String KEY_PREFIX = "mes:auth:blacklist:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    public void add(String jti, long ttlMs) {
        if (jti == null || jti.isBlank() || ttlMs <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(key(jti), "1", ttlMs, TimeUnit.MILLISECONDS);
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }

}
