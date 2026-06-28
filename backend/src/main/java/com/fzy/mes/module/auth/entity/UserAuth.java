package com.fzy.mes.module.auth.entity;

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
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_auth")
public class UserAuth {

	@NotNull(groups = Update.class, message = "认证ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotBlank(groups = Create.class, message = "登录账号不能为空")
	@Size(max = 50, message = "登录账号长度不能超过50")
	private String username;

	@NotBlank(groups = Create.class, message = "密码不能为空")
	@Size(max = 100, message = "密码长度不能超过100")
	private String password;

	@NotNull(groups = Create.class, message = "关联用户ID不能为空")
	private Long userId;

	@NotNull(groups = Create.class, message = "启用状态不能为空")
	@Min(value = 0, message = "启用状态只能为0或1")
	@Max(value = 1, message = "启用状态只能为0或1")
	private Integer enabled;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
