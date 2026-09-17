package com.fzy.mes.mq.consumer;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.report.dto.ReportSubmitMessage;
import com.fzy.mes.module.report.service.ReportProcessService;
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
 * 报工 Topic 消费：事务落库 + 状态机 + 触发 ERP 回传（D24~D26）。
 */
@Slf4j
@Component
public class ReportSubmitMessageListener implements MessageListenerConcurrently {

	@Autowired
	private ReportProcessService reportProcessService;
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages,
			ConsumeConcurrentlyContext context) {
		for (MessageExt message : messages) {
			String body = new String(message.getBody(), StandardCharsets.UTF_8);
			try {
				ReportSubmitMessage payload = objectMapper.readValue(body, ReportSubmitMessage.class);
				log.info("消费报工消息, msgId={}, keys={}, reportId={}",
						message.getMsgId(), message.getKeys(), payload.getReportId());
				reportProcessService.process(payload);
			} catch (BusinessException e) {
				log.warn("报工消费业务失败，将重试, msgId={}, err={}", message.getMsgId(), e.getMessage());
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			} catch (Exception e) {
				log.error("报工消费异常，将重试, msgId={}", message.getMsgId(), e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

}
