package com.example.creatorsettlement.dto.response;

import com.example.creatorsettlement.domain.Settlement;
import com.example.creatorsettlement.domain.SettlementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class  SettlementResponse {

    private Long id;
    private Long creatorId;
    private String creatorName;
    private String yearMonth;
    private SettlementStatus status;
    private BigDecimal commissionRate;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netSalesAmount;
    private BigDecimal commission;
    private BigDecimal settlementAmount;
    private int saleCount;
    private int cancelCount;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;

    public static SettlementResponse from(Settlement s) {
        return SettlementResponse.builder()
                .id(s.getId())
                .creatorId(s.getCreator().getId())
                .creatorName(s.getCreator().getName())
                .yearMonth(s.getYearMonth())
                .status(s.getStatus())
                .commissionRate(s.getCommissionRate())
                .totalSalesAmount(s.getTotalSalesAmount())
                .totalRefundAmount(s.getTotalRefundAmount())
                .netSalesAmount(s.getNetSalesAmount())
                .commission(s.getCommission())
                .settlementAmount(s.getSettlementAmount())
                .saleCount(s.getSaleCount())
                .cancelCount(s.getCancelCount())
                .confirmedAt(s.getConfirmedAt())
                .paidAt(s.getPaidAt())
                .build();
    }
}
