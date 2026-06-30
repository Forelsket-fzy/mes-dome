package com.fzy.mes.module.auth.controller;

import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.auth.dto.LoginRequest;
import com.fzy.mes.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/login")
    public Result<Map<String,String>> login(@RequestBody @Valid LoginRequest req) {
        System.out.println(req.toString());
        return Result.success(authService.login(req));
    }


}
