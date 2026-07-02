package com.fzy.mes.module.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
@TableName("work_order")
public class WorkOrder {

	@NotNull(groups = Update.class, message = "工单ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotBlank(groups = Create.class, message = "ERP订单号不能为空")
	@Size(max = 50, message = "ERP订单号长度不能超过50")
	private String erpOrderNo;

	@Size(max = 30, message = "ERP状态长度不能超过30")
	private String erpStatus;

	@NotBlank(groups = Create.class, message = "产品编码不能为空")
	@Size(max = 50, message = "产品编码长度不能超过50")
	private String productCode;

	@Size(max = 100, message = "产品名称长度不能超过100")
	private String productName;

	@NotNull(groups = Create.class, message = "计划数量不能为空")
	@Min(value = 1, message = "计划数量至少为1")
	private Integer planQty;

	@Min(value = 0, message = "完成数量不能为负数")
	private Integer completedQty;

	@Min(value = 0, message = "工单状态无效")
	@Max(value = 6, message = "工单状态无效")
	private Integer status;

	private LocalDateTime dueDate;

	@Size(max = 200, message = "取消原因长度不能超过200")
	private String cancelReason;

	@Version
	private Integer version;

	private Long createdBy;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
