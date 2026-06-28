package com.fzy.mes.module.integration.entity;

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
@TableName("integration_log")
public class IntegrationLog {

	@NotNull(groups = Update.class, message = "日志ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotBlank(groups = Create.class, message = "业务类型不能为空")
	@Size(max = 30, message = "业务类型长度不能超过30")
	private String bizType;

	@NotBlank(groups = Create.class, message = "业务ID不能为空")
	@Size(max = 50, message = "业务ID长度不能超过50")
	private String bizId;

	@NotBlank(groups = Create.class, message = "幂等键不能为空")
	@Size(max = 100, message = "幂等键长度不能超过100")
	private String idempotentKey;

	@NotBlank(groups = Create.class, message = "目标系统不能为空")
	@Size(max = 30, message = "目标系统长度不能超过30")
	private String targetSystem;

	@Min(value = 0, message = "集成状态无效")
	@Max(value = 3, message = "集成状态无效")
	private Integer status;

	private String payload;

	@Min(value = 0, message = "重试次数不能为负数")
	private Integer retryCount;

	@Size(max = 500, message = "错误信息长度不能超过500")
	private String errorMsg;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
