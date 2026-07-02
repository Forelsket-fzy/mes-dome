package com.fzy.mes.module.erp.service;


import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;

public interface ErpWorkOrderService {

    public ErpPushWorkOrderResponse push(ErpPushWorkOrderRequest req);

}
