package com.fzy.mes.module.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
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
@TableName("dispatch_audit_log")
public class DispatchAuditLog {

	@NotNull(groups = Update.class, message = "审计日志ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "派工记录ID不能为空")
	private Long dispatchId;

	@NotNull(groups = Create.class, message = "操作人ID不能为空")
	private Long actionBy;

	private String snapshotJson;

	private LocalDateTime createdAt;

}
