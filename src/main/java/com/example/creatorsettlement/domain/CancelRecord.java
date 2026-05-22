package com.example.creatorsettlement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancel_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_record_id", nullable = false, unique = true)
    private SaleRecord saleRecord;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private LocalDateTime cancelledAt;

    @Builder
    public CancelRecord(SaleRecord saleRecord, BigDecimal refundAmount, LocalDateTime cancelledAt) {
        this.saleRecord = saleRecord;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
    }
}
