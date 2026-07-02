package com.fzy.mes.module.erp.controller;

import com.fzy.mes.common.module.vo.Result;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.service.ErpWorkOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ErpWorkOrderController {

    @Autowired
    private ErpWorkOrderService erpWorkOrderService;

    @PostMapping("/erp/work-orders")
    public Result<ErpPushWorkOrderResponse> pushWorkOrder(
            @RequestBody @Valid ErpPushWorkOrderRequest erpPushWorkOrderRequest) {
        return Result.success(erpWorkOrderService.push(erpPushWorkOrderRequest));
    }

}
