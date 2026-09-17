package com.fzy.mes.module.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.cache.service.CacheService;
import com.fzy.mes.module.quality.service.QualityInspectionService;
import com.fzy.mes.module.report.dto.DefectItemRequest;
import com.fzy.mes.module.report.dto.ErpCallbackMessage;
import com.fzy.mes.module.report.dto.ReportSubmitMessage;
import com.fzy.mes.module.report.entity.DefectRecord;
import com.fzy.mes.module.report.entity.ProductionReport;
import com.fzy.mes.module.report.mapper.DefectRecordMapper;
import com.fzy.mes.module.report.mapper.ProductionReportMapper;
import com.fzy.mes.module.report.service.ReportProcessService;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStateMachine;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import com.fzy.mes.mq.producer.MesRocketMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReportProcessServiceImpl implements ReportProcessService {

	private static final int REPORT_SUCCESS = 1;
	private static final int REPORT_FAILED = 2;
	private static final int TASK_IN_PROGRESS = 1;
	private static final int TASK_COMPLETED = 2;

	@Value("${mes.redis.lock.task-report-ttl-seconds:30}")
	private long taskLockLeaseSeconds;

	@Autowired
	private ProductionReportMapper productionReportMapper;
	@Autowired
	private DefectRecordMapper defectRecordMapper;
	@Autowired
	private OperationTaskMapper operationTaskMapper;
	@Autowired
	private WorkOrderMapper workOrderMapper;
	@Autowired
	private WorkOrderStateMachine workOrderStateMachine;
	@Autowired
	private QualityInspectionService qualityInspectionService;
	@Autowired
	private RedissonClient redissonClient;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private CacheService cacheService;
	@Autowired(required = false)
	private MesRocketMqProducer mesRocketMqProducer;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void process(ReportSubmitMessage message) {
		if (message == null || message.getReportId() == null || message.getTaskId() == null) {
			throw new BusinessException("报工消息不完整");
		}

		ProductionReport report = productionReportMapper.selectById(message.getReportId());
		if (report == null) {
			throw new BusinessException("报工记录不存在: " + message.getReportId());
		}
		if (Integer.valueOf(REPORT_SUCCESS).equals(report.getStatus())) {
			log.info("报工已成功处理，跳过幂等, reportId={}", report.getId());
			return;
		}

		RLock lock = redissonClient.getLock(RedisCacheKeys.taskReportLock(message.getTaskId()));
		boolean locked = false;
		try {
			locked = lock.tryLock(5, taskLockLeaseSeconds, TimeUnit.SECONDS);
			if (!locked) {
				throw new BusinessException("获取任务锁失败，稍后重试");
			}
			doProcessLocked(message, report);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException("消费报工被中断");
		} finally {
			if (locked && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private void doProcessLocked(ReportSubmitMessage message, ProductionReport report) {
		report = productionReportMapper.selectById(report.getId());
		if (report != null && Integer.valueOf(REPORT_SUCCESS).equals(report.getStatus())) {
			return;
		}

		OperationTask task = operationTaskMapper.selectById(message.getTaskId());
		if (task == null) {
			markFailed(message.getReportId(), "工序任务不存在");
			throw new BusinessException("工序任务不存在");
		}

		WorkOrder workOrder = workOrderMapper.selectById(task.getWorkOrderId());
		if (workOrder == null) {
			markFailed(message.getReportId(), "工单不存在");
			throw new BusinessException("工单不存在");
		}

		int goodQty = message.getGoodQty() == null ? 0 : message.getGoodQty();
		int defectQty = message.getDefectQty() == null ? 0 : message.getDefectQty();
		int reportQty = goodQty + defectQty;
		if (reportQty <= 0) {
			markFailed(message.getReportId(), "报工数量无效");
			throw new BusinessException("报工数量无效");
		}

		int planQty = task.getPlanQty() == null ? 0 : task.getPlanQty();
		int completedQty = task.getCompletedQty() == null ? 0 : task.getCompletedQty();
		int remaining = planQty - completedQty;
		if (reportQty > remaining) {
			markFailed(message.getReportId(), "超报：剩余=" + remaining);
			throw new BusinessException("报工数量超过剩余可报数量");
		}

		int newCompleted = completedQty + reportQty;
		task.setCompletedQty(newCompleted);
		task.setStatus(newCompleted >= planQty ? TASK_COMPLETED : TASK_IN_PROGRESS);
		int updated = operationTaskMapper.updateById(task);
		if (updated == 0) {
			throw new BusinessException("工序乐观锁冲突，稍后重试");
		}

		saveDefects(message.getReportId(), message.getDefects());

		boolean lastOperation = isLastOperation(task);
		boolean lastOperationDone = lastOperation && newCompleted >= planQty;

		int woPlan = workOrder.getPlanQty() == null ? 0 : workOrder.getPlanQty();
		int woCompleted = workOrder.getCompletedQty() == null ? 0 : workOrder.getCompletedQty();
		if (lastOperation) {
			woCompleted = Math.min(woPlan, newCompleted);
			workOrder.setCompletedQty(woCompleted);
		} else if (woCompleted <= 0 && goodQty > 0) {
			// 非末工序：有产出时先把工单完成数记为良品累计（演示用，不超计划）
			woCompleted = Math.min(woPlan, goodQty);
			workOrder.setCompletedQty(woCompleted);
		}

		WorkOrderStatus current = WorkOrderStatus.fromCode(workOrder.getStatus());
		WorkOrderStatus next = workOrderStateMachine.resolveAfterReport(
				current, woPlan, workOrder.getCompletedQty() == null ? 0 : workOrder.getCompletedQty(),
				lastOperationDone);
		workOrder.setStatus(next.getCode());
		int woUpdated = workOrderMapper.updateById(workOrder);
		if (woUpdated == 0) {
			throw new BusinessException("工单乐观锁冲突，稍后重试");
		}

		ProductionReport success = new ProductionReport();
		success.setId(message.getReportId());
		success.setStatus(REPORT_SUCCESS);
		success.setErrorMsg(null);
		productionReportMapper.updateById(success);

		qualityInspectionService.createIfDefect(
				message.getReportId(), workOrder.getId(), task.getId(), defectQty);

		cacheService.deleteKey(RedisCacheKeys.WORK_ORDER_STATUS_STATS);

		sendErpCallback(message, workOrder, task, goodQty, defectQty, next, lastOperationDone);

		log.info("报工消费成功, reportId={}, taskId={}, woStatus={}, lastDone={}",
				message.getReportId(), task.getId(), next, lastOperationDone);
	}

	private void saveDefects(Long reportId, List<DefectItemRequest> defects) {
		Long exist = defectRecordMapper.selectCount(
				Wrappers.<DefectRecord>lambdaQuery().eq(DefectRecord::getReportId, reportId));
		if (exist != null && exist > 0) {
			return;
		}
		if (defects == null || defects.isEmpty()) {
			return;
		}
		for (DefectItemRequest item : defects) {
			DefectRecord record = DefectRecord.builder()
					.reportId(reportId)
					.reasonId(item.getReasonId())
					.qty(item.getQty())
					.build();
			defectRecordMapper.insert(record);
		}
	}

	private boolean isLastOperation(OperationTask task) {
		List<OperationTask> tasks = operationTaskMapper.selectList(
				Wrappers.<OperationTask>lambdaQuery()
						.eq(OperationTask::getWorkOrderId, task.getWorkOrderId()));
		OperationTask maxSeq = tasks.stream()
				.max(Comparator.comparing(OperationTask::getSeq))
				.orElse(task);
		return maxSeq.getId().equals(task.getId());
	}

	private void sendErpCallback(ReportSubmitMessage message, WorkOrder workOrder, OperationTask task,
			int goodQty, int defectQty, WorkOrderStatus next, boolean completed) {
		if (mesRocketMqProducer == null) {
			log.warn("Producer 未启用，跳过 ERP 回传, reportId={}", message.getReportId());
			return;
		}
		ErpCallbackMessage callback = new ErpCallbackMessage();
		callback.setReportId(message.getReportId());
		callback.setRequestId(message.getRequestId());
		callback.setWorkOrderId(workOrder.getId());
		callback.setErpOrderNo(workOrder.getErpOrderNo());
		callback.setTaskId(task.getId());
		callback.setGoodQty(goodQty);
		callback.setDefectQty(defectQty);
		callback.setWorkOrderStatus(next.getCode());
		callback.setCompleted(completed);
		String body = objectMapper.writeValueAsString(callback);
		mesRocketMqProducer.sendErpCallback(body, "report:" + message.getReportId());
	}

	private void markFailed(Long reportId, String errorMsg) {
		ProductionReport patch = new ProductionReport();
		patch.setId(reportId);
		patch.setStatus(REPORT_FAILED);
		patch.setErrorMsg(errorMsg == null ? "消费失败" : truncate(errorMsg, 500));
		productionReportMapper.updateById(patch);
	}

	private String truncate(String text, int max) {
		return text.length() <= max ? text : text.substring(0, max);
	}

}
