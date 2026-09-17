package com.fzy.mes.module.quality.service;

/**
 * QMS Mock：报工含不良时生成检验任务（D32）。
 */
public interface QualityInspectionService {

	void createIfDefect(Long reportId, Long workOrderId, Long taskId, int defectQty);

}
