package com.fzy.mes.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

	@NotNull(groups = Update.class, message = "用户ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotBlank(groups = Create.class, message = "真实姓名不能为空")
	@Size(max = 50, message = "真实姓名长度不能超过50")
	private String realName;

	@Email(message = "邮箱格式不正确")
	@Size(max = 100, message = "邮箱长度不能超过100")
	private String email;

	@Size(max = 20, message = "手机号长度不能超过20")
	private String phone;

	@NotNull(groups = Create.class, message = "技能等级不能为空")
	@Min(value = 1, message = "技能等级最小为1")
	@Max(value = 5, message = "技能等级最大为5")
	private Integer skillLevel;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
