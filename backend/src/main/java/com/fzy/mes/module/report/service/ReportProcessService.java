package com.fzy.mes.module.report.service;

import com.fzy.mes.module.report.dto.ReportSubmitMessage;

/**
 * 报工 MQ 消费落库（D24）+ 状态机（D25）+ 触发 ERP 回传（D26）。
 */
public interface ReportProcessService {

	void process(ReportSubmitMessage message);

}
