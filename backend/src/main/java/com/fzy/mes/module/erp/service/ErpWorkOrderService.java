package com.fzy.mes.module.erp.service;

import com.fzy.mes.module.erp.dto.ErpCloseWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderRequest;
import com.fzy.mes.module.erp.dto.ErpPushWorkOrderResponse;
import com.fzy.mes.module.erp.vo.ErpCloseWorkOrderResponse;
import jakarta.validation.Valid;

public interface ErpWorkOrderService {

    ErpPushWorkOrderResponse push(ErpPushWorkOrderRequest req);

    ErpCloseWorkOrderResponse close(String erpOrderNo, @Valid ErpCloseWorkOrderRequest req);

}
