package com.fzy.mes.mq.config;

import com.fzy.mes.mq.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(RocketMqProperties.class)
@ConditionalOnProperty(prefix = "mes.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqProducerConfig {

	@Bean(destroyMethod = "shutdown")
	public DefaultMQProducer defaultMQProducer(RocketMqProperties properties) throws MQClientException {
		RocketMqProperties.Producer conf = properties.getProducer();
		DefaultMQProducer producer = new DefaultMQProducer(conf.getGroup());
		producer.setNamesrvAddr(properties.getNameServer());
		producer.setSendMsgTimeout(conf.getSendMessageTimeout());
		producer.setRetryTimesWhenSendFailed(conf.getRetryTimesWhenSendFailed());
		producer.setMaxMessageSize(conf.getMaxMessageSize());
		producer.start();
		log.info("RocketMQ Producer 已启动, nameServer={}, group={}",
				properties.getNameServer(), conf.getGroup());
		return producer;
	}

}
