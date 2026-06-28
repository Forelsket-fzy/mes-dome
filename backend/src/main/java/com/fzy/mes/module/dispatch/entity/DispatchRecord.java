package com.fzy.mes.module.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dispatch_record")
public class DispatchRecord {

	@NotNull(groups = Update.class, message = "派工记录ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "工序任务ID不能为空")
	private Long taskId;

	@NotNull(groups = Create.class, message = "操作人ID不能为空")
	private Long operatorId;

	@NotNull(groups = Create.class, message = "被派工人ID不能为空")
	private Long assigneeId;

	@Min(value = 1, message = "派工模式无效")
	@Max(value = 3, message = "派工模式无效")
	private Integer mode;

	private LocalDateTime createdAt;

}
