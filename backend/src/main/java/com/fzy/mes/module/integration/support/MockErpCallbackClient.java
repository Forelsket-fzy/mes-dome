package com.fzy.mes.module.integration.support;

import com.fzy.mes.module.report.dto.ErpCallbackMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock ERP：本地演示用「假回传」，始终成功（可在失败演示时手动改 integration_log）。
 */
@Slf4j
@Component
public class MockErpCallbackClient {

	public void callback(ErpCallbackMessage message) {
		log.info("Mock ERP 回传成功, erpOrderNo={}, reportId={}, woStatus={}, completed={}",
				message.getErpOrderNo(),
				message.getReportId(),
				message.getWorkOrderStatus(),
				message.getCompleted());
	}

}
