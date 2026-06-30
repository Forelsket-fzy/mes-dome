package com.fzy.mes.module.auth.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {

    @NotEmpty(message = "用户名不能为空")
    private String username;
    @NotEmpty
    private String password;

}
