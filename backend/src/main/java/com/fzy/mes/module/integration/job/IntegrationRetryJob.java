package com.fzy.mes.module.integration.job;

import com.fzy.mes.module.integration.service.IntegrationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * D29：定时扫描失败的 integration_log 自动重试。
 */
@Slf4j
@Component
public class IntegrationRetryJob {

	@Autowired
	private IntegrationLogService integrationLogService;

	@Scheduled(fixedDelayString = "${mes.integration.retry-interval-ms:300000}")
	public void retryFailed() {
		int count = integrationLogService.scanAndRetryFailed(20);
		if (count > 0) {
			log.info("集成日志自动重试完成, count={}", count);
		}
	}

}
