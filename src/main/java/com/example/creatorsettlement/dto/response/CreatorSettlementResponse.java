package com.example.creatorsettlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreatorSettlementResponse {

    private Long creatorId;
    private String creatorName;
    private String yearMonth;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netSalesAmount;
    private BigDecimal commission;
    private BigDecimal settlementAmount;
    private long saleCount;
    private long cancelCount;
}
