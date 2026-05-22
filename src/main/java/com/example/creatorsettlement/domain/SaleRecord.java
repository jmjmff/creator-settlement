package com.example.creatorsettlement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(nullable = false)
    private LocalDateTime paymentAt;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Builder
    public SaleRecord(Course course, String studentId, BigDecimal paymentAmount, LocalDateTime paymentAt) {
        this.course = course;
        this.studentId = studentId;
        this.paymentAmount = paymentAmount;
        this.paymentAt = paymentAt;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
