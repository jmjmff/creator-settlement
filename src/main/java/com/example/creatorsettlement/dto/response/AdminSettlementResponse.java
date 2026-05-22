package com.example.creatorsettlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AdminSettlementResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private List<CreatorSummary> creatorSummaries;
    private BigDecimal totalSettlementAmount;

    @Getter
    @Builder
    public static class CreatorSummary {
        private Long creatorId;
        private String creatorName;
        private BigDecimal totalSalesAmount;
        private BigDecimal totalRefundAmount;
        private BigDecimal netSalesAmount;
        private BigDecimal settlementAmount;
        private long saleCount;
        private long cancelCount;
    }
}
