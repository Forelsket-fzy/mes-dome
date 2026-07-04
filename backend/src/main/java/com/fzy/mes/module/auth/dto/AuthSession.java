package com.fzy.mes.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fzy.mes.common.validation.Create;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuthSession {

    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String realName;

    private String phone;

    private Integer skillLevel;

    private Boolean enabled;

    private List<String> role;

}
