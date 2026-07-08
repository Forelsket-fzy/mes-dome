package com.fzy.mes.module.cache;

public final class RedisCacheKeys {

    public static final String WORK_ORDER_STATUS_STATS = "mes:stats:wo:status";

    private RedisCacheKeys() {
    }

    public static String userSession(String username) {
        return "mes:user:" + username;
    }

}
