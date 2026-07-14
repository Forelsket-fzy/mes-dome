package com.fzy.mes.module.dispatch.vo;

import lombok.Data;

@Data
public class DispatchResponse {

    private Long dispatchId;
    private Long taskId;
    private Long workOrderId;
    private Integer workOrderStatus;

}
