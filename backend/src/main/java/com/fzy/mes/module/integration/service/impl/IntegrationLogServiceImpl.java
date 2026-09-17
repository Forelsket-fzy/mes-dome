package com.fzy.mes.module.integration.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.integration.dto.IntegrationLogQuery;
import com.fzy.mes.module.integration.entity.IntegrationLog;
import com.fzy.mes.module.integration.mapper.IntegrationLogMapper;
import com.fzy.mes.module.integration.service.IntegrationLogService;
import com.fzy.mes.module.integration.support.MockErpCallbackClient;
import com.fzy.mes.module.report.dto.ErpCallbackMessage;
import com.fzy.mes.mq.producer.MesRocketMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
public class IntegrationLogServiceImpl implements IntegrationLogService {

	public static final String BIZ_REPORT_CALLBACK = "REPORT_CALLBACK";
	public static final String TARGET_ERP = "ERP";
	private static final int STATUS_PENDING = 0;
	private static final int STATUS_SUCCESS = 1;
	private static final int STATUS_FAILED = 2;
	private static final int STATUS_RETRYING = 3;
	private static final int MAX_AUTO_RETRY = 5;

	@Autowired
	private IntegrationLogMapper integrationLogMapper;
	@Autowired
	private MockErpCallbackClient mockErpCallbackClient;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired(required = false)
	private MesRocketMqProducer mesRocketMqProducer;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void handleErpCallback(ErpCallbackMessage message) {
		if (message == null || message.getReportId() == null) {
			throw new BusinessException("ERP 回传消息不完整");
		}
		String idempotentKey = "report:" + message.getReportId();
		IntegrationLog existing = integrationLogMapper.selectOne(
				Wrappers.<IntegrationLog>lambdaQuery()
						.eq(IntegrationLog::getIdempotentKey, idempotentKey));
		if (existing != null && Integer.valueOf(STATUS_SUCCESS).equals(existing.getStatus())) {
			log.info("ERP 回传已成功，跳过幂等, key={}", idempotentKey);
			return;
		}

		IntegrationLog logEntity = existing;
		if (logEntity == null) {
			logEntity = IntegrationLog.builder()
					.bizType(BIZ_REPORT_CALLBACK)
					.bizId(String.valueOf(message.getReportId()))
					.idempotentKey(idempotentKey)
					.targetSystem(TARGET_ERP)
					.status(STATUS_PENDING)
					.payload(objectMapper.writeValueAsString(message))
					.retryCount(0)
					.build();
			try {
				integrationLogMapper.insert(logEntity);
			} catch (DuplicateKeyException e) {
				logEntity = integrationLogMapper.selectOne(
						Wrappers.<IntegrationLog>lambdaQuery()
								.eq(IntegrationLog::getIdempotentKey, idempotentKey));
				if (logEntity != null && Integer.valueOf(STATUS_SUCCESS).equals(logEntity.getStatus())) {
					return;
				}
			}
		}

		try {
			mockErpCallbackClient.callback(message);
			IntegrationLog patch = new IntegrationLog();
			patch.setId(logEntity.getId());
			patch.setStatus(STATUS_SUCCESS);
			patch.setErrorMsg(null);
			patch.setPayload(objectMapper.writeValueAsString(message));
			integrationLogMapper.updateById(patch);
		} catch (Exception e) {
			IntegrationLog patch = new IntegrationLog();
			patch.setId(logEntity.getId());
			patch.setStatus(STATUS_FAILED);
			patch.setErrorMsg(truncate(e.getMessage(), 500));
			int retry = logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount();
			patch.setRetryCount(retry + 1);
			integrationLogMapper.updateById(patch);
			throw new BusinessException("Mock ERP 回传失败: " + e.getMessage());
		}
	}

	@Override
	public Page<IntegrationLog> page(IntegrationLogQuery query) {
		Page<IntegrationLog> page = new Page<>(query.getCurrent(), query.getSize());
		var wrapper = Wrappers.<IntegrationLog>lambdaQuery()
				.eq(StringUtils.hasText(query.getBizType()), IntegrationLog::getBizType, query.getBizType())
				.eq(StringUtils.hasText(query.getTargetSystem()), IntegrationLog::getTargetSystem, query.getTargetSystem())
				.eq(query.getStatus() != null, IntegrationLog::getStatus, query.getStatus())
				.orderByDesc(IntegrationLog::getId);
		return integrationLogMapper.selectPage(page, wrapper);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public IntegrationLog retry(Long id) {
		IntegrationLog logEntity = integrationLogMapper.selectById(id);
		if (logEntity == null) {
			throw new BusinessException("集成日志不存在");
		}
		if (Integer.valueOf(STATUS_SUCCESS).equals(logEntity.getStatus())) {
			return logEntity;
		}
		if (!BIZ_REPORT_CALLBACK.equals(logEntity.getBizType())) {
			throw new BusinessException("暂仅支持 REPORT_CALLBACK 重试");
		}

		IntegrationLog patch = new IntegrationLog();
		patch.setId(id);
		patch.setStatus(STATUS_RETRYING);
		int retry = logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount();
		patch.setRetryCount(retry + 1);
		integrationLogMapper.updateById(patch);

		ErpCallbackMessage message = objectMapper.readValue(logEntity.getPayload(), ErpCallbackMessage.class);
		if (mesRocketMqProducer != null) {
			String body = objectMapper.writeValueAsString(message);
			mesRocketMqProducer.sendErpCallback(body, logEntity.getIdempotentKey());
		} else {
			handleErpCallback(message);
		}
		return integrationLogMapper.selectById(id);
	}

	@Override
	public int scanAndRetryFailed(int limit) {
		List<IntegrationLog> failed = integrationLogMapper.selectList(
				Wrappers.<IntegrationLog>lambdaQuery()
						.eq(IntegrationLog::getStatus, STATUS_FAILED)
						.eq(IntegrationLog::getBizType, BIZ_REPORT_CALLBACK)
						.lt(IntegrationLog::getRetryCount, MAX_AUTO_RETRY)
						.orderByAsc(IntegrationLog::getId)
						.last("LIMIT " + Math.max(1, limit)));
		int count = 0;
		for (IntegrationLog item : failed) {
			try {
				retry(item.getId());
				count++;
			} catch (Exception e) {
				log.warn("自动重试集成日志失败, id={}, err={}", item.getId(), e.getMessage());
			}
		}
		return count;
	}

	private String truncate(String text, int max) {
		if (text == null) {
			return null;
		}
		return text.length() <= max ? text : text.substring(0, max);
	}

}
