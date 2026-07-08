package com.fzy.mes.module.cache.service.impl;

import com.fzy.mes.module.cache.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T getValue(String key, Class<T> type) {
        Object cached = getValue(key);
        if (cached == null) {
            return null;
        }
        if (type.isInstance(cached)) {
            return type.cast(cached);
        }
        return objectMapper.convertValue(cached, type);
    }

    @Override
    public void setValueWithExpire(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

}
