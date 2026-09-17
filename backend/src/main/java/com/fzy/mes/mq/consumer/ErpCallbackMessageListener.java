package com.fzy.mes.mq.consumer;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.integration.service.IntegrationLogService;
import com.fzy.mes.module.report.dto.ErpCallbackMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ERP 回传 Topic 消费：写 integration_log + Mock ERP（D26）。
 */
@Slf4j
@Component
public class ErpCallbackMessageListener implements MessageListenerConcurrently {

	@Autowired
	private IntegrationLogService integrationLogService;
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages,
			ConsumeConcurrentlyContext context) {
		for (MessageExt message : messages) {
			String body = new String(message.getBody(), StandardCharsets.UTF_8);
			try {
				ErpCallbackMessage payload = objectMapper.readValue(body, ErpCallbackMessage.class);
				log.info("消费 ERP 回传, msgId={}, keys={}, reportId={}",
						message.getMsgId(), message.getKeys(), payload.getReportId());
				integrationLogService.handleErpCallback(payload);
			} catch (BusinessException e) {
				log.warn("ERP 回传业务失败，将重试, msgId={}, err={}", message.getMsgId(), e.getMessage());
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			} catch (Exception e) {
				log.error("ERP 回传消费异常，将重试, msgId={}", message.getMsgId(), e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

}
