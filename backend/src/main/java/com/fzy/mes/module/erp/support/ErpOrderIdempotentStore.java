package com.fzy.mes.module.erp.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class ErpOrderIdempotentStore {

    private static final String KEY_PREFIX = "mes:erp:order:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${mes.redis.idempotent.erp-order-ttl-days:7}")
    private long erpOrderTtlDays;

    /**
     * SET NX：首次推单返回 true，重复推单返回 false。
     */
    public boolean tryMark(String erpOrderNo) {
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(key(erpOrderNo), "PENDING", erpOrderTtlDays, TimeUnit.DAYS);
        return Boolean.TRUE.equals(ok);
    }

    public void bindWorkOrderId(String erpOrderNo, Long workOrderId) {
        redisTemplate.opsForValue().set(key(erpOrderNo), workOrderId, erpOrderTtlDays, TimeUnit.DAYS);
    }

    private String key(String erpOrderNo) {
        return KEY_PREFIX + erpOrderNo;
    }

}
