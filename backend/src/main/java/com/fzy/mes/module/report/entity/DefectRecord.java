package com.fzy.mes.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
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
@TableName("defect_record")
public class DefectRecord {

	@NotNull(groups = Update.class, message = "明细ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "报工记录ID不能为空")
	private Long reportId;

	@NotNull(groups = Create.class, message = "不良原因ID不能为空")
	private Long reasonId;

	@NotNull(groups = Create.class, message = "不良数量不能为空")
	@Min(value = 1, message = "不良数量至少为1")
	private Integer qty;

	private LocalDateTime createdAt;

}
