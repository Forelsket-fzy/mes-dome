package com.fzy.mes.module.report.service;

import com.fzy.mes.common.exception.BusinessException;
import com.fzy.mes.module.report.dto.DefectItemRequest;
import com.fzy.mes.module.report.dto.ReportSubmitRequest;
import com.fzy.mes.module.report.entity.DefectReason;
import com.fzy.mes.module.report.mapper.DefectReasonMapper;
import com.fzy.mes.module.report.service.impl.ReportServiceImpl;
import com.fzy.mes.module.report.vo.ReportValidateResponse;
import com.fzy.mes.module.workorder.entity.OperationTask;
import com.fzy.mes.module.workorder.entity.WorkOrder;
import com.fzy.mes.module.workorder.mapper.OperationTaskMapper;
import com.fzy.mes.module.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceValidateTest {

	@Mock
	private OperationTaskMapper operationTaskMapper;
	@Mock
	private WorkOrderMapper workOrderMapper;
	@Mock
	private DefectReasonMapper defectReasonMapper;

	@InjectMocks
	private ReportServiceImpl reportService;

	private ReportSubmitRequest baseRequest;
	private OperationTask task;
	private WorkOrder workOrder;

	@BeforeEach
	void setUp() {
		baseRequest = new ReportSubmitRequest();
		baseRequest.setTaskId(10L);
		baseRequest.setGoodQty(2);
		baseRequest.setDefectQty(0);

		task = new OperationTask();
		task.setId(10L);
		task.setWorkOrderId(1L);
		task.setOperationCode("OP010");
		task.setPlanQty(10);
		task.setCompletedQty(3);
		task.setStatus(0);
		task.setAssignedTo(3L);

		workOrder = new WorkOrder();
		workOrder.setId(1L);
		workOrder.setStatus(1);
	}

	@Test
	void validateSubmit_success() {
		when(operationTaskMapper.selectById(10L)).thenReturn(task);
		when(workOrderMapper.selectById(1L)).thenReturn(workOrder);

		ReportValidateResponse response = reportService.validateSubmit(baseRequest, 3L, "req-001");

		assertTrue(response.isValidated());
		assertEquals(7, response.getRemainingQty());
		assertEquals(2, response.getReportQty());
	}

	@Test
	void validateSubmit_rejectBlankRequestId() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 3L, "  "));
		assertTrue(ex.getMessage().contains("X-Request-Id"));
	}

	@Test
	void validateSubmit_rejectZeroQty() {
		baseRequest.setGoodQty(0);
		baseRequest.setDefectQty(0);
		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 3L, "req-001"));
		assertTrue(ex.getMessage().contains("必须大于0"));
	}

	@Test
	void validateSubmit_rejectOtherWorker() {
		when(operationTaskMapper.selectById(10L)).thenReturn(task);
		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 99L, "req-001"));
		assertTrue(ex.getMessage().contains("只能为自己"));
	}

	@Test
	void validateSubmit_rejectExceedRemaining() {
		when(operationTaskMapper.selectById(10L)).thenReturn(task);
		when(workOrderMapper.selectById(1L)).thenReturn(workOrder);
		baseRequest.setGoodQty(8);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 3L, "req-001"));
		assertTrue(ex.getMessage().contains("超过工序剩余"));
	}

	@Test
	void validateSubmit_rejectClosedWorkOrder() {
		workOrder.setStatus(5);
		when(operationTaskMapper.selectById(10L)).thenReturn(task);
		when(workOrderMapper.selectById(1L)).thenReturn(workOrder);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 3L, "req-001"));
		assertTrue(ex.getMessage().contains("禁止报工"));
	}

	@Test
	void validateSubmit_rejectDefectSumMismatch() {
		DefectItemRequest item = new DefectItemRequest();
		item.setReasonId(1L);
		item.setQty(1);
		baseRequest.setDefectQty(2);
		baseRequest.setDefects(List.of(item));

		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 3L, "req-001"));
		assertTrue(ex.getMessage().contains("不良明细合计"));
	}

	@Test
	void validateSubmit_withDefect_success() {
		DefectItemRequest item = new DefectItemRequest();
		item.setReasonId(1L);
		item.setQty(1);
		baseRequest.setGoodQty(1);
		baseRequest.setDefectQty(1);
		baseRequest.setDefects(List.of(item));

		DefectReason reason = new DefectReason();
		reason.setId(1L);
		reason.setCode("DIM-001");
		reason.setOperationCode(null);

		when(operationTaskMapper.selectById(10L)).thenReturn(task);
		when(workOrderMapper.selectById(1L)).thenReturn(workOrder);
		when(defectReasonMapper.selectById(1L)).thenReturn(reason);

		ReportValidateResponse response = reportService.validateSubmit(baseRequest, 3L, "req-def");
		assertTrue(response.isValidated());
		assertEquals(2, response.getReportQty());
	}

	@Test
	void validateSubmit_rejectCompletedTask() {
		task.setStatus(2);
		when(operationTaskMapper.selectById(anyLong())).thenReturn(task);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> reportService.validateSubmit(baseRequest, 3L, "req-001"));
		assertTrue(ex.getMessage().contains("已完工"));
	}

}
