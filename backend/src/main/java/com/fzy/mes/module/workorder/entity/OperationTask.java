package com.fzy.mes.module.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
import com.fzy.mes.module.erp.dto.OperationDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_task")
public class OperationTask extends OperationDto {

	@NotNull(groups = Update.class, message = "工序任务ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "工单ID不能为空")
	private Long workOrderId;

	@NotBlank(groups = Create.class, message = "工序编码不能为空")
	@Size(max = 30, message = "工序编码长度不能超过30")
	private String operationCode;

	@Size(max = 100, message = "工序名称长度不能超过100")
	private String operationName;

	@NotNull(groups = Create.class, message = "工序序号不能为空")
	@Min(value = 1, message = "工序序号至少为1")
	private Integer seq;

	@NotNull(groups = Create.class, message = "工序计划数量不能为空")
	@Min(value = 1, message = "工序计划数量至少为1")
	private Integer planQty;

	@Min(value = 0, message = "工序完成数量不能为负数")
	private Integer completedQty;

	@Min(value = 0, message = "工序状态无效")
	@Max(value = 2, message = "工序状态无效")
	private Integer status;

	private Integer priority;

	private LocalDateTime plannedStart;

	private Long assignedTo;

	@Version
	private Integer version;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
