package com.fzy.mes.module.quality.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.module.quality.entity.QualityInspectionTask;
import com.fzy.mes.module.quality.mapper.QualityInspectionTaskMapper;
import com.fzy.mes.module.quality.service.QualityInspectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QualityInspectionServiceImpl implements QualityInspectionService {

	private static final int QC_PENDING = 0;

	@Autowired
	private QualityInspectionTaskMapper qualityInspectionTaskMapper;

	@Override
	public void createIfDefect(Long reportId, Long workOrderId, Long taskId, int defectQty) {
		if (defectQty <= 0 || reportId == null) {
			return;
		}
		Long exists = qualityInspectionTaskMapper.selectCount(
				Wrappers.<QualityInspectionTask>lambdaQuery()
						.eq(QualityInspectionTask::getReportId, reportId));
		if (exists != null && exists > 0) {
			return;
		}
		QualityInspectionTask task = QualityInspectionTask.builder()
				.reportId(reportId)
				.workOrderId(workOrderId)
				.taskId(taskId)
				.status(QC_PENDING)
				.resultRemark("报工含不良，自动生成质检任务（QMS Mock）")
				.build();
		qualityInspectionTaskMapper.insert(task);
		log.info("已创建质检任务, qcId={}, reportId={}, defectQty={}", task.getId(), reportId, defectQty);
	}

}
