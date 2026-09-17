package com.fzy.mes.mq.config;

import com.fzy.mes.mq.RocketMqProperties;
import com.fzy.mes.mq.consumer.ErpCallbackMessageListener;
import com.fzy.mes.mq.consumer.ReportSubmitMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "mes.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqConsumerConfig {

	@Bean(destroyMethod = "shutdown")
	public DefaultMQPushConsumer reportSubmitConsumer(
			RocketMqProperties properties,
			ReportSubmitMessageListener listener) throws MQClientException {
		RocketMqProperties.Consumer conf = properties.getConsumer();
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(conf.getReportGroup());
		consumer.setNamesrvAddr(properties.getNameServer());
		consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
		consumer.setConsumeThreadMin(conf.getConsumeThreadMin());
		consumer.setConsumeThreadMax(conf.getConsumeThreadMax());
		consumer.subscribe(
				properties.getTopic().getReportSubmit(),
				properties.getTag().getReportSubmit());
		consumer.registerMessageListener(listener);
		consumer.start();
		log.info("RocketMQ Consumer 已启动, nameServer={}, group={}, topic={}, tag={}",
				properties.getNameServer(),
				conf.getReportGroup(),
				properties.getTopic().getReportSubmit(),
				properties.getTag().getReportSubmit());
		return consumer;
	}

	@Bean(destroyMethod = "shutdown")
	public DefaultMQPushConsumer erpCallbackConsumer(
			RocketMqProperties properties,
			ErpCallbackMessageListener listener) throws MQClientException {
		RocketMqProperties.Consumer conf = properties.getConsumer();
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(conf.getErpCallbackGroup());
		consumer.setNamesrvAddr(properties.getNameServer());
		consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
		consumer.setConsumeThreadMin(conf.getConsumeThreadMin());
		consumer.setConsumeThreadMax(conf.getConsumeThreadMax());
		consumer.subscribe(
				properties.getTopic().getErpCallback(),
				properties.getTag().getErpCallback());
		consumer.registerMessageListener(listener);
		consumer.start();
		log.info("RocketMQ Consumer 已启动, nameServer={}, group={}, topic={}, tag={}",
				properties.getNameServer(),
				conf.getErpCallbackGroup(),
				properties.getTopic().getErpCallback(),
				properties.getTag().getErpCallback());
		return consumer;
	}

}
