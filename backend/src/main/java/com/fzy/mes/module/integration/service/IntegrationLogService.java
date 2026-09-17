package com.fzy.mes.module.integration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fzy.mes.module.integration.dto.IntegrationLogQuery;
import com.fzy.mes.module.integration.entity.IntegrationLog;
import com.fzy.mes.module.report.dto.ErpCallbackMessage;

public interface IntegrationLogService {

	void handleErpCallback(ErpCallbackMessage message);

	Page<IntegrationLog> page(IntegrationLogQuery query);

	IntegrationLog retry(Long id);

	int scanAndRetryFailed(int limit);

}
