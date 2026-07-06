package com.fzy.mes.module.erp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ErpCloseWorkOrderRequest {

    public enum CloseType {
        NORMAL,
        CANCELLED
    }

    private String erpStatus;

    @NotNull(message = "关单类型不能为空")
    private CloseType closeType;

    private String cancelReason;

}
