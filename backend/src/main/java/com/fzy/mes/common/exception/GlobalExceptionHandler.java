package com.fzy.mes.common.exception;

import com.fzy.mes.common.module.vo.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理 @Valid 校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.badRequest(msg);
    }

    // 兜底
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        return Result.error( "系统繁忙");
    }

}
