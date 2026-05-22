package com.example.creatorsettlement.dto.response;

import com.example.creatorsettlement.domain.CancelRecord;
import com.example.creatorsettlement.domain.SaleRecord;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SaleRecordResponse {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long creatorId;
    private String creatorName;
    private String studentId;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentAt;
    private boolean cancelled;
    private CancelInfo cancelInfo;

    @Getter
    @Builder
    public static class CancelInfo {
        private Long cancelRecordId;
        private BigDecimal refundAmount;
        private LocalDateTime cancelledAt;
    }

    public static SaleRecordResponse of(SaleRecord sale, CancelRecord cancel) {
        CancelInfo cancelInfo = null;
        if (cancel != null) {
            cancelInfo = CancelInfo.builder()
                    .cancelRecordId(cancel.getId())
                    .refundAmount(cancel.getRefundAmount())
                    .cancelledAt(cancel.getCancelledAt())
                    .build();
        }
        return SaleRecordResponse.builder()
                .id(sale.getId())
                .courseId(sale.getCourse().getId())
                .courseTitle(sale.getCourse().getTitle())
                .creatorId(sale.getCourse().getCreator().getId())
                .creatorName(sale.getCourse().getCreator().getName())
                .studentId(sale.getStudentId())
                .paymentAmount(sale.getPaymentAmount())
                .paymentAt(sale.getPaymentAt())
                .cancelled(sale.isCancelled())
                .cancelInfo(cancelInfo)
                .build();
    }
}
