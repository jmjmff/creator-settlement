package com.example.creatorsettlement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class SaleRecordCreateRequest {

    @NotNull(message = "강의 ID는 필수입니다")
    private Long courseId;

    @NotBlank(message = "수강생 ID는 필수입니다")
    private String studentId;

    @NotNull(message = "결제 금액은 필수입니다")
    @Positive(message = "결제 금액은 0보다 커야 합니다")
    private BigDecimal paymentAmount;

    @NotNull(message = "결제 일시는 필수입니다")
    private LocalDateTime paymentAt;
}
