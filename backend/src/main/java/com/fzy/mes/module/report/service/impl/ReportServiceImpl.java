package com.fzy.mes.module.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.report.dto.DefectItemRequest;
import com.fzy.mes.module.report.dto.ReportSubmitRequest;
import com.fzy.mes.module.report.entity.DefectReason;
import com.fzy.mes.module.report.mapper.DefectReasonMapper;
import com.fzy.mes.module.report.service.ReportService;
import com.fzy.mes.module.report.vo.DefectReasonVO;
import com.fzy.mes.module.report.vo.ReportValidateResponse;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import com.fzy.mes.module.workorder.statemachine.WorkOrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

	private static final int TASK_STATUS_COMPLETED = 2;
	private static final int REQUEST_ID_MAX_LEN = 50;

	@Autowired
	private OperationTaskMapper operationTaskMapper;
	@Autowired
	private WorkOrderMapper workOrderMapper;
	@Autowired
	private DefectReasonMapper defectReasonMapper;

	@Override
	public ReportValidateResponse validateSubmit(ReportSubmitRequest request, Long operatorId, String requestId) {
		validateRequestId(requestId);
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

		int planQty = task.getPlanQty() == null ? 0 : task.getPlanQty();
		int completedQty = task.getCompletedQty() == null ? 0 : task.getCompletedQty();
		int remainingQty = planQty - completedQty;
		if (remainingQty <= 0) {
			throw new BusinessException("工序剩余可报数量为0");
		}
		if (reportQty > remainingQty) {
			throw new BusinessException("报工数量超过工序剩余数量，剩余=" + remainingQty);
		}

		validateDefectReasons(defects, task.getOperationCode());

		return ReportValidateResponse.builder()
				.validated(true)
				.requestId(requestId.trim())
				.taskId(task.getId())
				.workOrderId(workOrder.getId())
				.reportQty(reportQty)
				.remainingQty(remainingQty)
				.message("校验通过，待 D23 接入幂等与 MQ 异步受理")
				.build();
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

	private void validateRequestId(String requestId) {
		if (!StringUtils.hasText(requestId)) {
			throw new BusinessException("X-Request-Id 不能为空");
		}
		String trimmed = requestId.trim();
		if (trimmed.length() > REQUEST_ID_MAX_LEN) {
			throw new BusinessException("X-Request-Id 长度不能超过" + REQUEST_ID_MAX_LEN);
		}
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

}
