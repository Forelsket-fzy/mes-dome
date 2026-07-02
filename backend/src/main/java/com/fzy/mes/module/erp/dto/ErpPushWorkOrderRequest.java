package com.fzy.mes.module.erp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpPushWorkOrderRequest {

    @NotBlank(message = "ERP订单号不能为空")
    private String erpOrderNo;

    private String erpStatus;

    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量至少为1")
    private Integer planQty;

    @NotEmpty(message = "工序列表不能为空")
    @Valid
    private List<OperationDto> operations;

    private LocalDateTime dueDate;

}
