package com.fzy.mes.module.cache.service;

import java.util.concurrent.TimeUnit;

public interface CacheService {

    void setValue(String key, Object value);

    Object getValue(String key);

    <T> T getValue(String key, Class<T> type);

    void setValueWithExpire(String key, Object value, long timeout, TimeUnit unit);

    void deleteKey(String key);
}
