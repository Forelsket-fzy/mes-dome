package com.fzy.mes.mq.producer;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.mq.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 业务侧统一发送入口；D23 报工 / D26 ERP 回传复用。
 */
@Slf4j
@Component
@ConditionalOnBean(DefaultMQProducer.class)
public class MesRocketMqProducer {

	@Autowired
	private DefaultMQProducer defaultMQProducer;
	@Autowired
	private RocketMqProperties rocketMqProperties;

	public SendResult sendReportSubmit(String body, String keys) {
		return send(
				rocketMqProperties.getTopic().getReportSubmit(),
				rocketMqProperties.getTag().getReportSubmit(),
				body,
				keys);
	}

	public SendResult sendErpCallback(String body, String keys) {
		return send(
				rocketMqProperties.getTopic().getErpCallback(),
				rocketMqProperties.getTag().getErpCallback(),
				body,
				keys);
	}

	public SendResult send(String topic, String tag, String body, String keys) {
		try {
			Message message = new Message(
					topic,
					tag,
					keys,
					body.getBytes(StandardCharsets.UTF_8));
			SendResult result = defaultMQProducer.send(message);
			log.info("RocketMQ 发送成功, topic={}, tag={}, keys={}, msgId={}",
					topic, tag, keys, result.getMsgId());
			return result;
		} catch (Exception e) {
			log.error("RocketMQ 发送失败, topic={}, tag={}, keys={}", topic, tag, keys, e);
			throw new BusinessException("消息发送失败: " + e.getMessage());
		}
	}

}
