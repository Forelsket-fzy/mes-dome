package com.fzy.mes.module.cache;

public final class RedisCacheKeys {

	public static final String WORK_ORDER_STATUS_STATS = "mes:stats:wo:status";

	private static final String REPORT_IDEMPOTENT_PREFIX = "mes:report:idempotent:";
	private static final String TASK_REPORT_LOCK_PREFIX = "mes:lock:task:";

	private RedisCacheKeys() {
	}

	public static String userSession(String username) {
		return "mes:user:" + username;
	}

	public static String reportIdempotent(String requestId) {
		return REPORT_IDEMPOTENT_PREFIX + requestId;
	}

	public static String taskReportLock(Long taskId) {
		return TASK_REPORT_LOCK_PREFIX + taskId;
	}

}
