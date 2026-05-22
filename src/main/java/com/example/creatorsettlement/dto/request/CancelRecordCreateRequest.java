package com.example.creatorsettlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CancelRecordCreateRequest {

    @NotNull(message = "판매 내역 ID는 필수입니다")
    private Long saleRecordId;

    @NotNull(message = "환불 금액은 필수입니다")
    @Positive(message = "환불 금액은 0보다 커야 합니다")
    private BigDecimal refundAmount;

    @NotNull(message = "취소 일시는 필수입니다")
    private LocalDateTime cancelledAt;
}
