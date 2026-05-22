package com.example.creatorsettlement.service;

import com.example.creatorsettlement.domain.CancelRecord;
import com.example.creatorsettlement.domain.Course;
import com.example.creatorsettlement.domain.Creator;
import com.example.creatorsettlement.domain.SaleRecord;
import com.example.creatorsettlement.dto.response.CreatorSettlementResponse;
import com.example.creatorsettlement.exception.NotFoundException;
import com.example.creatorsettlement.repository.CancelRecordRepository;
import com.example.creatorsettlement.repository.CreatorRepository;
import com.example.creatorsettlement.repository.SaleRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private SaleRecordRepository saleRecordRepository;
    @Mock
    private CancelRecordRepository cancelRecordRepository;

    private Creator creator;
    private Course course;

    @BeforeEach
    void setUp() {
        creator = Creator.builder().name("김민준").email("kim@example.com").build();
        course = Course.builder().title("Spring Boot 완성").creator(creator).build();
    }

    @Test
    @DisplayName("크리에이터 월별 정산 - 정상 계산")
    void getCreatorSettlement_success() {
        SaleRecord sale1 = SaleRecord.builder().course(course).studentId(101L)
                .paymentAmount(new BigDecimal("50000"))
                .paymentAt(LocalDateTime.of(2025, 3, 5, 10, 0)).build();
        SaleRecord sale2 = SaleRecord.builder().course(course).studentId(102L)
                .paymentAmount(new BigDecimal("35000"))
                .paymentAt(LocalDateTime.of(2025, 3, 12, 16, 0)).build();

        CancelRecord cancel = CancelRecord.builder().saleRecord(sale1)
                .refundAmount(new BigDecimal("50000"))
                .cancelledAt(LocalDateTime.of(2025, 3, 11, 15, 0)).build();

        given(creatorRepository.findById(1L)).willReturn(Optional.of(creator));
        given(saleRecordRepository.findByCreatorIdAndPeriod(eq(1L), any(), any()))
                .willReturn(List.of(sale1, sale2));
        given(cancelRecordRepository.findByCreatorIdAndPeriod(eq(1L), any(), any()))
                .willReturn(List.of(cancel));

        CreatorSettlementResponse response = settlementService.getCreatorSettlement(1L, "2025-03");

        // 총 판매: 85000, 환불: 50000, 순 판매: 35000
        // 수수료(20%): 7000, 정산 예정: 28000
        assertThat(response.getTotalSalesAmount()).isEqualByComparingTo("85000");
        assertThat(response.getTotalRefundAmount()).isEqualByComparingTo("50000");
        assertThat(response.getNetSalesAmount()).isEqualByComparingTo("35000");
        assertThat(response.getCommission()).isEqualByComparingTo("7000");
        assertThat(response.getSettlementAmount()).isEqualByComparingTo("28000");
        assertThat(response.getSaleCount()).isEqualTo(2);
        assertThat(response.getCancelCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("크리에이터 월별 정산 - 판매/취소 없음")
    void getCreatorSettlement_empty() {
        given(creatorRepository.findById(1L)).willReturn(Optional.of(creator));
        given(saleRecordRepository.findByCreatorIdAndPeriod(any(), any(), any())).willReturn(List.of());
        given(cancelRecordRepository.findByCreatorIdAndPeriod(any(), any(), any())).willReturn(List.of());

        CreatorSettlementResponse response = settlementService.getCreatorSettlement(1L, "2025-03");

        assertThat(response.getTotalSalesAmount()).isEqualByComparingTo("0");
        assertThat(response.getSettlementAmount()).isEqualByComparingTo("0");
        assertThat(response.getSaleCount()).isZero();
    }

    @Test
    @DisplayName("크리에이터 월별 정산 - 존재하지 않는 크리에이터")
    void getCreatorSettlement_creatorNotFound() {
        given(creatorRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.getCreatorSettlement(99L, "2025-03"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("크리에이터를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("정산 수수료 20% 반올림 검증")
    void commission_roundHalfUp() {
        // 순 판매 33333 * 20% = 6666.6 → 반올림 → 6667, 정산 = 26666
        SaleRecord sale = SaleRecord.builder().course(course).studentId(101L)
                .paymentAmount(new BigDecimal("33333"))
                .paymentAt(LocalDateTime.of(2025, 3, 5, 10, 0)).build();

        given(creatorRepository.findById(1L)).willReturn(Optional.of(creator));
        given(saleRecordRepository.findByCreatorIdAndPeriod(any(), any(), any())).willReturn(List.of(sale));
        given(cancelRecordRepository.findByCreatorIdAndPeriod(any(), any(), any())).willReturn(List.of());

        CreatorSettlementResponse response = settlementService.getCreatorSettlement(1L, "2025-03");

        assertThat(response.getCommission()).isEqualByComparingTo("6667");
        assertThat(response.getSettlementAmount()).isEqualByComparingTo("26666");
    }
}
