package com.example.creatorsettlement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "commission_rate_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false)
    private LocalDate appliedFrom;

    // null = 현재까지 적용 중
    @Column
    private LocalDate appliedTo;

    @Builder
    public CommissionRateHistory(BigDecimal rate, LocalDate appliedFrom, LocalDate appliedTo) {
        this.rate = rate;
        this.appliedFrom = appliedFrom;
        this.appliedTo = appliedTo;
    }
}
