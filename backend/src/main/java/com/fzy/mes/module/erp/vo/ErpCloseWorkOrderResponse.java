package com.fzy.mes.module.erp.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErpCloseWorkOrderResponse {

    private long workOrderId;
    private int previousStatus;
    private int currentStatus;
    private boolean duplicate;
    private boolean conflict;

}
