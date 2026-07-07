package com.fzy.mes.module.workorder.vo;

import lombok.Data;

@Data
public class WorkOrderStatusVO {

    private int status;
    private String statusLabel;
    private long count;

}
