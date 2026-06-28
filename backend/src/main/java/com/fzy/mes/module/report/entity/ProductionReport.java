package com.fzy.mes.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
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
@TableName("production_report")
public class ProductionReport {

	@NotNull(groups = Update.class, message = "报工记录ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "工序任务ID不能为空")
	private Long taskId;

	@NotBlank(groups = Create.class, message = "请求ID不能为空")
	@Size(max = 50, message = "请求ID长度不能超过50")
	private String requestId;

	@NotNull(groups = Create.class, message = "良品数量不能为空")
	@Min(value = 0, message = "良品数量不能为负数")
	private Integer goodQty;

	@Min(value = 0, message = "不良品数量不能为负数")
	private Integer defectQty;

	@NotNull(groups = Create.class, message = "操作人ID不能为空")
	private Long operatorId;

	@Min(value = 0, message = "报工状态无效")
	@Max(value = 2, message = "报工状态无效")
	private Integer status;

	@Size(max = 500, message = "错误信息长度不能超过500")
	private String errorMsg;

	private LocalDateTime reportedAt;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
