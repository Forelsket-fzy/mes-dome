package com.fzy.mes.module.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@TableName("quality_inspection_task")
public class QualityInspectionTask {

	@NotNull(groups = Update.class, message = "检验任务ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "报工记录ID不能为空")
	private Long reportId;

	@NotNull(groups = Create.class, message = "工单ID不能为空")
	private Long workOrderId;

	@NotNull(groups = Create.class, message = "工序任务ID不能为空")
	private Long taskId;

	@Min(value = 0, message = "质检状态无效")
	@Max(value = 2, message = "质检状态无效")
	private Integer status;

	private Long inspectorId;

	@Size(max = 500, message = "检验备注长度不能超过500")
	private String resultRemark;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
