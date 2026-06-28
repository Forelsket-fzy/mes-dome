package com.fzy.mes.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
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
@TableName("defect_reason")
public class DefectReason {

	@NotNull(groups = Update.class, message = "原因ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotBlank(groups = Create.class, message = "原因编码不能为空")
	@Size(max = 30, message = "原因编码长度不能超过30")
	private String code;

	@NotBlank(groups = Create.class, message = "原因名称不能为空")
	@Size(max = 100, message = "原因名称长度不能超过100")
	private String name;

	@NotBlank(groups = Create.class, message = "不良类型不能为空")
	@Size(max = 30, message = "不良类型长度不能超过30")
	private String defectType;

	@Size(max = 30, message = "适用工序编码长度不能超过30")
	private String operationCode;

	private LocalDateTime createdAt;

}
