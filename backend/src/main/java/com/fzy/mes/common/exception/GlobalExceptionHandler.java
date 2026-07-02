package com.fzy.mes.common.exception;

import com.fzy.mes.common.module.vo.Result;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.badRequest(msg != null && !msg.isBlank() ? msg : "参数校验失败");
    }


    @ExceptionHandler(BadCredentialsException.class)
    public Result<String> handleBadCredentials(BadCredentialsException e) {
        return Result.unauthorized("用户名或密码错误");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<String> handleUsernameNotFound(UsernameNotFoundException e) {
        return Result.unauthorized("用户名或密码错误");
    }

    @ExceptionHandler(DisabledException.class)
    public Result<String> handleDisabled(DisabledException e) {
        return Result.unauthorized("账号已禁用");
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public Result<String> handleInternalAuth(InternalAuthenticationServiceException e) {
        Throwable cause = e.getCause();
        if (cause instanceof UsernameNotFoundException) {
            return Result.unauthorized("用户名或密码错误");
        }
        if (cause instanceof DisabledException) {
            return Result.unauthorized("账号已禁用");
        }
        return Result.unauthorized("认证失败");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusiness(BusinessException e) {
        return Result.badRequest(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        return Result.error("系统繁忙");
    }

}
