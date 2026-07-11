package com.fzy.mes.module.dispatch.vo;

import lombok.Data;

@Data
public class WorkerListItemVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer skillLevel;

}
