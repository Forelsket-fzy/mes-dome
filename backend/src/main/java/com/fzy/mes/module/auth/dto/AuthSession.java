package com.fzy.mes.module.auth.dto;

import com.fzy.mes.common.validation.Create;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthSession {

    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private Integer skillLevel;

    private Boolean enabled;

    private String role;

}
