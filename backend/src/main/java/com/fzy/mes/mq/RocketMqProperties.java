package com.fzy.mes.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对齐 application.yml 的 mes.rocketmq.*，以及 docker-compose 中 namesrv:9876。
 */
@Data
@ConfigurationProperties(prefix = "mes.rocketmq")
public class RocketMqProperties {

	/** 为 false 时不创建 Producer/Consumer，避免本地未起 MQ 时启动失败 */
	private boolean enabled = true;

	/** NameServer，如 124.223.171.40:9876 或 127.0.0.1:9876 */
	private String nameServer;

	private Producer producer = new Producer();
	private Consumer consumer = new Consumer();
	private Topic topic = new Topic();
	private Tag tag = new Tag();

	@Data
	public static class Producer {
		private String group = "mes-producer-group";
		private int sendMessageTimeout = 3000;
		private int retryTimesWhenSendFailed = 2;
		private int maxMessageSize = 4194304;
	}

	@Data
	public static class Consumer {
		/** 报工落库消费组 */
		private String reportGroup = "mes-report-consumer-group";
		/** ERP 回传消费组 */
		private String erpCallbackGroup = "mes-erp-callback-consumer-group";
		private int consumeThreadMin = 5;
		private int consumeThreadMax = 20;
	}

	@Data
	public static class Topic {
		private String reportSubmit = "mes_report_submit";
		private String erpCallback = "mes_erp_callback";
	}

	@Data
	public static class Tag {
		private String reportSubmit = "submit";
		private String erpCallback = "callback";
	}

}
