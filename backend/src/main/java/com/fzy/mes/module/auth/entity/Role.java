package com.fzy.mes.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role")
public class Role {

	@NotNull(groups = Update.class, message = "角色ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotBlank(groups = Create.class, message = "角色编码不能为空")
	@Size(max = 30, message = "角色编码长度不能超过30")
	private String roleCode;

	@NotBlank(groups = Create.class, message = "角色名称不能为空")
	@Size(max = 50, message = "角色名称长度不能超过50")
	private String roleName;

	private LocalDateTime createdAt;

}
