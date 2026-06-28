package com.fzy.mes.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fzy.mes.common.validation.Create;
import com.fzy.mes.common.validation.Update;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@TableName("user_role")
public class UserRole {

	@NotNull(groups = Update.class, message = "关联ID不能为空")
	@TableId(type = IdType.AUTO)
	private Long id;

	@NotNull(groups = Create.class, message = "用户ID不能为空")
	private Long userId;

	@NotNull(groups = Create.class, message = "角色ID不能为空")
	private Long roleId;

}
