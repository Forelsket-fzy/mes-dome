package com.fzy.mes.module.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErpPushWorkOrderResponse {

    private Long id;
    private boolean duplicated;

}
