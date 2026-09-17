package com.fzy.mes.module.report.support;

import com.fzy.mes.module.cache.RedisCacheKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * 报工 API 层幂等：SET NX on mes:report:idempotent:{requestId}
 */
@Repository
public class ReportIdempotentStore {

	private static final String PENDING = "PENDING";

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Value("${mes.redis.idempotent.report-request-ttl-hours:24}")
	private long reportTtlHours;

	/**
	 * 首次受理返回 true；同一 requestId 重复调用返回 false。
	 */
	public boolean tryMark(String requestId) {
		Boolean ok = redisTemplate.opsForValue()
				.setIfAbsent(RedisCacheKeys.reportIdempotent(requestId), PENDING, reportTtlHours, TimeUnit.HOURS);
		return Boolean.TRUE.equals(ok);
	}

	public void bindReportId(String requestId, Long reportId) {
		redisTemplate.opsForValue()
				.set(RedisCacheKeys.reportIdempotent(requestId), reportId, reportTtlHours, TimeUnit.HOURS);
	}

	/**
	 * @return reportId；仍为 PENDING 或无 key 时返回 null
	 */
	public Long getReportId(String requestId) {
		Object value = redisTemplate.opsForValue().get(RedisCacheKeys.reportIdempotent(requestId));
		if (value == null || PENDING.equals(String.valueOf(value))) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void release(String requestId) {
		redisTemplate.delete(RedisCacheKeys.reportIdempotent(requestId));
	}

}
