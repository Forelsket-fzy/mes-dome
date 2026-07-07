package com.fzy.mes.module.workorder.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkOrderStatusStatsItemsVO {

    private long total;

    private List<WorkOrderStatusVO> items = new ArrayList<>();

}
