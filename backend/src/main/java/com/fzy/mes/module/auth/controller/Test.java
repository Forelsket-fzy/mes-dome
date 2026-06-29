package com.fzy.mes.module.auth.controller;


import com.fzy.mes.common.module.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class Test {

    @GetMapping("/Test")
    public Result<String> Test1(){
        System.out.println("Test");
        return Result.success("test");
    }

}
