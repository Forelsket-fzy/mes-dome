package com.fzy.mes.module.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.cache.RedisCacheKeys;
import com.fzy.mes.module.report.dto.DefectItemRequest;
import com.fzy.mes.module.report.dto.ReportSubmitMessage;
import com.fzy.mes.module.report.dto.ReportSubmitRequest;
import com.fzy.mes.module.report.entity.DefectReason;
import com.fzy.mes.module.report.entity.ProductionReport;
import com.fzy.mes.module.report.mapper.DefectReasonMapper;
import com.fzy.mes.module.report.mapper.ProductionReportMapper;
import com.fzy.mes.module.report.service.ReportService;
import com.fzy.mes.module.report.support.ReportIdempotentStore;
import com.fzy.mes.module.report.vo.DefectReasonVO;
import com.fzy.mes.module.report.vo.ReportAcceptResponse;
import com.fzy.mes.module.report.vo.ReportStatusVO;
import com.fzy.mes.module.report.vo.ReportValidateResponse;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import com.fzy.mes.mq.producer.MesRocketMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

	private static final int TASK_STATUS_COMPLETED = 2;
	private static final int REPORT_STATUS_PROCESSING = 0;
	private static final int REPORT_STATUS_FAILED = 2;
	private static final int REQUEST_ID_MAX_LEN = 50;

	@Value("${mes.redis.lock.task-report-ttl-seconds:30}")
	private long taskLockLeaseSeconds;

	@Autowired
	private OperationTaskMapper operationTaskMapper;
	@Autowired
	private WorkOrderMapper workOrderMapper;
	@Autowired
	private DefectReasonMapper defectReasonMapper;
	@Autowired
	private ProductionReportMapper productionReportMapper;
	@Autowired
	private ReportIdempotentStore reportIdempotentStore;
	@Autowired
	private RedissonClient redissonClient;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired(required = false)
	private MesRocketMqProducer mesRocketMqProducer;

	@Override
	public ReportValidateResponse validateSubmit(ReportSubmitRequest request, Long operatorId, String requestId) {
		ValidateContext ctx = doValidate(request, operatorId, requestId);
		return ReportValidateResponse.builder()
				.validated(true)
				.requestId(ctx.requestId())
				.taskId(ctx.task().getId())
				.workOrderId(ctx.workOrder().getId())
				.reportQty(ctx.reportQty())
				.remainingQty(ctx.remainingQty())
				.message("校验通过")
				.build();
	}

	@Override
	public ReportAcceptResponse submit(ReportSubmitRequest request, Long operatorId, String requestId) {
		ValidateContext ctx = doValidate(request, operatorId, requestId);
		String rid = ctx.requestId();

		// 1) Redis 幂等：已受理过则直接返回原单；若上次 MQ 失败(status=2)则允许同 requestId 重发
		if (!reportIdempotentStore.tryMark(rid)) {
			ProductionReport existing = findReportByRequestId(rid);
			if (existing != null && Integer.valueOf(REPORT_STATUS_FAILED).equals(existing.getStatus())) {
				return retrySendAfterFailure(existing, ctx, request, operatorId, rid);
			}
			ReportAcceptResponse accepted = toAcceptResponse(existing);
			if (accepted != null) {
				return accepted;
			}
			// Redis 有坑但 DB 无单（常见于清库未清 Redis）：释放脏 Key 后重新占坑
			log.warn("幂等 Key 存在但报工单不存在，释放后重新受理, requestId={}", rid);
			reportIdempotentStore.release(rid);
			if (!reportIdempotentStore.tryMark(rid)) {
				throw new BusinessException("报工处理中，请勿重复提交");
			}
		}

		RLock lock = redissonClient.getLock(RedisCacheKeys.taskReportLock(ctx.task().getId()));
		boolean locked = false;
		Long reportId = null;
		try {
			locked = lock.tryLock(3, taskLockLeaseSeconds, TimeUnit.SECONDS);
			if (!locked) {
				reportIdempotentStore.release(rid);
				throw new BusinessException("任务繁忙，请稍后重试");
			}

			// 2) 锁内二次数量校验（防并发超报受理）
			OperationTask freshTask = operationTaskMapper.selectById(ctx.task().getId());
			recheckRemaining(freshTask, ctx.reportQty());

			// 3) 插入处理中报工单
			ProductionReport report = new ProductionReport();
			report.setTaskId(ctx.task().getId());
			report.setRequestId(rid);
			report.setGoodQty(request.getGoodQty());
			report.setDefectQty(request.getDefectQty() == null ? 0 : request.getDefectQty());
			report.setOperatorId(operatorId);
			report.setStatus(REPORT_STATUS_PROCESSING);
			report.setReportedAt(LocalDateTime.now());
			try {
				productionReportMapper.insert(report);
			} catch (DuplicateKeyException e) {
				ProductionReport dup = findReportByRequestId(rid);
				if (dup != null && Integer.valueOf(REPORT_STATUS_FAILED).equals(dup.getStatus())) {
					return retrySendAfterFailure(dup, ctx, request, operatorId, rid);
				}
				ReportAcceptResponse accepted = toAcceptResponse(dup);
				if (accepted != null) {
					return accepted;
				}
				throw new BusinessException("请勿重复报工");
			}
			reportId = report.getId();
			reportIdempotentStore.bindReportId(rid, reportId);

			// 4) 发 MQ
			if (mesRocketMqProducer == null) {
				markFailed(reportId, "RocketMQ 未启用，无法异步受理");
				throw new BusinessException("消息队列未启用，报工受理失败");
			}
			ReportSubmitMessage message = buildMessage(reportId, rid, ctx, request, operatorId);
			String body = objectMapper.writeValueAsString(message);
			try {
				mesRocketMqProducer.sendReportSubmit(body, rid);
			} catch (BusinessException e) {
				markFailed(reportId, e.getMessage());
				throw e;
			}

			return ReportAcceptResponse.builder()
					.reportId(reportId)
					.requestId(rid)
					.status(REPORT_STATUS_PROCESSING)
					.message("报工已受理，请轮询处理结果")
					.build();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (reportId == null) {
				reportIdempotentStore.release(rid);
			}
			throw new BusinessException("获取任务锁被中断");
		} catch (BusinessException e) {
			if (reportId == null) {
				reportIdempotentStore.release(rid);
			}
			throw e;
		} catch (RuntimeException e) {
			if (reportId == null) {
				reportIdempotentStore.release(rid);
			} else {
				markFailed(reportId, e.getMessage());
			}
			log.error("报工受理失败, requestId={}", rid, e);
			throw new BusinessException("报工受理失败: " + e.getMessage());
		} finally {
			if (locked && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	public ReportStatusVO getById(Long reportId) {
		ProductionReport report = productionReportMapper.selectById(reportId);
		if (report == null) {
			throw new BusinessException("报工记录不存在");
		}
		return toStatusVo(report);
	}

	@Override
	public List<DefectReasonVO> listDefectReasons(String operationCode) {
		var query = Wrappers.<DefectReason>lambdaQuery();
		if (StringUtils.hasText(operationCode)) {
			query.and(w -> w.isNull(DefectReason::getOperationCode)
					.or()
					.eq(DefectReason::getOperationCode, operationCode.trim()));
		}
		query.orderByAsc(DefectReason::getId);
		List<DefectReason> reasons = defectReasonMapper.selectList(query);
		return reasons.stream().map(this::toReasonVo).collect(Collectors.toList());
	}

	private ValidateContext doValidate(ReportSubmitRequest request, Long operatorId, String requestId) {
		String rid = normalizeRequestId(requestId);
		if (operatorId == null) {
			throw new BusinessException("操作人无效");
		}

		int goodQty = request.getGoodQty() == null ? 0 : request.getGoodQty();
		int defectQty = request.getDefectQty() == null ? 0 : request.getDefectQty();
		int reportQty = goodQty + defectQty;
		if (reportQty <= 0) {
			throw new BusinessException("报工数量必须大于0");
		}

		List<DefectItemRequest> defects = request.getDefects() == null ? List.of() : request.getDefects();
		validateDefectItems(defectQty, defects);

		OperationTask task = operationTaskMapper.selectById(request.getTaskId());
		if (task == null) {
			throw new BusinessException("工序任务不存在");
		}
		if (task.getStatus() != null && task.getStatus() == TASK_STATUS_COMPLETED) {
			throw new BusinessException("工序任务已完工，禁止报工");
		}
		if (task.getAssignedTo() == null) {
			throw new BusinessException("工序任务尚未派工，禁止报工");
		}
		if (!Objects.equals(task.getAssignedTo(), operatorId)) {
			throw new BusinessException("只能为自己被派工的任务报工");
		}

		WorkOrder workOrder = workOrderMapper.selectById(task.getWorkOrderId());
		if (workOrder == null) {
			throw new BusinessException("工单不存在");
		}
		WorkOrderStatus woStatus = WorkOrderStatus.fromCode(workOrder.getStatus());
		if (woStatus.isFinal() || woStatus == WorkOrderStatus.COMPLETED) {
			throw new BusinessException("工单当前状态禁止报工: " + woStatus.getName());
		}
		if (woStatus != WorkOrderStatus.ASSIGNED
				&& woStatus != WorkOrderStatus.IN_PROGRESS
				&& woStatus != WorkOrderStatus.PARTIAL_COMPLETED) {
			throw new BusinessException("工单当前状态不支持报工: " + woStatus.getName());
		}

		int remainingQty = remainingOf(task);
		if (remainingQty <= 0) {
			throw new BusinessException("工序剩余可报数量为0");
		}
		if (reportQty > remainingQty) {
			throw new BusinessException("报工数量超过工序剩余数量，剩余=" + remainingQty);
		}

		validateDefectReasons(defects, task.getOperationCode());
		return new ValidateContext(rid, task, workOrder, reportQty, remainingQty);
	}

	private void recheckRemaining(OperationTask task, int reportQty) {
		if (task == null) {
			throw new BusinessException("工序任务不存在");
		}
		if (task.getStatus() != null && task.getStatus() == TASK_STATUS_COMPLETED) {
			throw new BusinessException("工序任务已完工，禁止报工");
		}
		int remainingQty = remainingOf(task);
		if (reportQty > remainingQty) {
			throw new BusinessException("报工数量超过工序剩余数量，剩余=" + remainingQty);
		}
	}

	private int remainingOf(OperationTask task) {
		int planQty = task.getPlanQty() == null ? 0 : task.getPlanQty();
		int completedQty = task.getCompletedQty() == null ? 0 : task.getCompletedQty();
		return planQty - completedQty;
	}

	private ProductionReport findReportByRequestId(String requestId) {
		Long reportId = reportIdempotentStore.getReportId(requestId);
		ProductionReport report = null;
		if (reportId != null) {
			report = productionReportMapper.selectById(reportId);
		}
		if (report == null) {
			report = productionReportMapper.selectOne(
					Wrappers.<ProductionReport>lambdaQuery().eq(ProductionReport::getRequestId, requestId));
		}
		if (report != null) {
			reportIdempotentStore.bindReportId(requestId, report.getId());
		}
		return report;
	}

	private ReportAcceptResponse toAcceptResponse(ProductionReport report) {
		if (report == null) {
			return null;
		}
		return ReportAcceptResponse.builder()
				.reportId(report.getId())
				.requestId(report.getRequestId())
				.status(report.getStatus())
				.message(report.getStatus() != null && report.getStatus() == REPORT_STATUS_PROCESSING
						? "报工处理中（幂等返回）"
						: "报工记录已存在（幂等返回）")
				.build();
	}

	/**
	 * 上次因 MQ 发送失败落成 status=2 时，允许同一 requestId 重试发消息（不新建单据）。
	 */
	private ReportAcceptResponse retrySendAfterFailure(ProductionReport report, ValidateContext ctx,
			ReportSubmitRequest request, Long operatorId, String requestId) {
		if (mesRocketMqProducer == null) {
			throw new BusinessException("消息队列未启用，无法重试报工");
		}
		ProductionReport patch = new ProductionReport();
		patch.setId(report.getId());
		patch.setStatus(REPORT_STATUS_PROCESSING);
		patch.setErrorMsg(null);
		productionReportMapper.updateById(patch);

		ReportSubmitMessage message = buildMessage(report.getId(), requestId, ctx, request, operatorId);
		String body = objectMapper.writeValueAsString(message);
		try {
			mesRocketMqProducer.sendReportSubmit(body, requestId);
		} catch (BusinessException e) {
			markFailed(report.getId(), e.getMessage());
			throw e;
		}
		return ReportAcceptResponse.builder()
				.reportId(report.getId())
				.requestId(requestId)
				.status(REPORT_STATUS_PROCESSING)
				.message("报工已重新受理，请轮询处理结果")
				.build();
	}

	private ReportSubmitMessage buildMessage(Long reportId, String requestId, ValidateContext ctx,
			ReportSubmitRequest request, Long operatorId) {
		ReportSubmitMessage message = new ReportSubmitMessage();
		message.setReportId(reportId);
		message.setRequestId(requestId);
		message.setTaskId(ctx.task().getId());
		message.setWorkOrderId(ctx.workOrder().getId());
		message.setOperatorId(operatorId);
		message.setGoodQty(request.getGoodQty());
		message.setDefectQty(request.getDefectQty() == null ? 0 : request.getDefectQty());
		message.setDefects(request.getDefects() == null ? List.of() : request.getDefects());
		return message;
	}

	private void markFailed(Long reportId, String errorMsg) {
		ProductionReport patch = new ProductionReport();
		patch.setId(reportId);
		patch.setStatus(REPORT_STATUS_FAILED);
		patch.setErrorMsg(errorMsg == null ? "受理失败" : truncate(errorMsg, 500));
		productionReportMapper.updateById(patch);
	}

	private String truncate(String text, int max) {
		return text.length() <= max ? text : text.substring(0, max);
	}

	private String normalizeRequestId(String requestId) {
		if (!StringUtils.hasText(requestId)) {
			throw new BusinessException("X-Request-Id 不能为空");
		}
		String trimmed = requestId.trim();
		if (trimmed.length() > REQUEST_ID_MAX_LEN) {
			throw new BusinessException("X-Request-Id 长度不能超过" + REQUEST_ID_MAX_LEN);
		}
		return trimmed;
	}

	private void validateDefectItems(int defectQty, List<DefectItemRequest> defects) {
		if (defectQty == 0) {
			if (defects != null && !defects.isEmpty()) {
				throw new BusinessException("不良数量为0时不应提交不良明细");
			}
			return;
		}
		if (defects == null || defects.isEmpty()) {
			throw new BusinessException("存在不良品时必须填写不良原因明细");
		}

		int sum = 0;
		Set<Long> reasonIds = new HashSet<>();
		for (DefectItemRequest item : defects) {
			if (item.getReasonId() == null) {
				throw new BusinessException("不良原因ID不能为空");
			}
			if (item.getQty() == null || item.getQty() < 1) {
				throw new BusinessException("不良明细数量至少为1");
			}
			if (!reasonIds.add(item.getReasonId())) {
				throw new BusinessException("不良原因不能重复: " + item.getReasonId());
			}
			sum += item.getQty();
		}
		if (sum != defectQty) {
			throw new BusinessException("不良明细合计必须等于不良品数量");
		}
	}

	private void validateDefectReasons(List<DefectItemRequest> defects, String operationCode) {
		if (defects == null || defects.isEmpty()) {
			return;
		}
		for (DefectItemRequest item : defects) {
			DefectReason reason = defectReasonMapper.selectById(item.getReasonId());
			if (reason == null) {
				throw new BusinessException("不良原因不存在: " + item.getReasonId());
			}
			if (StringUtils.hasText(reason.getOperationCode())
					&& StringUtils.hasText(operationCode)
					&& !reason.getOperationCode().equals(operationCode)) {
				throw new BusinessException("不良原因不适用于当前工序: " + reason.getCode());
			}
		}
	}

	private DefectReasonVO toReasonVo(DefectReason reason) {
		DefectReasonVO vo = new DefectReasonVO();
		vo.setId(reason.getId());
		vo.setCode(reason.getCode());
		vo.setName(reason.getName());
		vo.setDefectType(reason.getDefectType());
		vo.setOperationCode(reason.getOperationCode());
		return vo;
	}

	private ReportStatusVO toStatusVo(ProductionReport report) {
		return ReportStatusVO.builder()
				.reportId(report.getId())
				.requestId(report.getRequestId())
				.taskId(report.getTaskId())
				.goodQty(report.getGoodQty())
				.defectQty(report.getDefectQty())
				.status(report.getStatus())
				.errorMsg(report.getErrorMsg())
				.reportedAt(report.getReportedAt())
				.build();
	}

	private record ValidateContext(
			String requestId,
			OperationTask task,
			WorkOrder workOrder,
			int reportQty,
			int remainingQty) {
	}

}
