package com.example.creatorsettlement.domain;

import com.example.creatorsettlement.exception.InvalidSettlementStatusException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"creator_id", "year_month"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(nullable = false)
    private String yearMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSalesAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRefundAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalesAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commission;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal settlementAmount;

    @Column(nullable = false)
    private int saleCount;

    @Column(nullable = false)
    private int cancelCount;

    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;

    @Builder
    public Settlement(Creator creator, String yearMonth, BigDecimal commissionRate,
                      BigDecimal totalSalesAmount, BigDecimal totalRefundAmount,
                      BigDecimal netSalesAmount, BigDecimal commission,
                      BigDecimal settlementAmount, int saleCount, int cancelCount) {
        this.creator = creator;
        this.yearMonth = yearMonth;
        this.status = SettlementStatus.PENDING;
        this.commissionRate = commissionRate;
        this.totalSalesAmount = totalSalesAmount;
        this.totalRefundAmount = totalRefundAmount;
        this.netSalesAmount = netSalesAmount;
        this.commission = commission;
        this.settlementAmount = settlementAmount;
        this.saleCount = saleCount;
        this.cancelCount = cancelCount;
    }

    public void confirm() {
        if (this.status != SettlementStatus.PENDING) {
            throw new InvalidSettlementStatusException(
                    "PENDING 상태에서만 확정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void pay() {
        if (this.status != SettlementStatus.CONFIRMED) {
            throw new InvalidSettlementStatusException(
                    "CONFIRMED 상태에서만 지급 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = SettlementStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
}
