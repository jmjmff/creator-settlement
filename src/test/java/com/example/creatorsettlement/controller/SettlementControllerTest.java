package com.example.creatorsettlement.controller;

import com.example.creatorsettlement.domain.Course;
import com.example.creatorsettlement.domain.Creator;
import com.example.creatorsettlement.domain.SaleRecord;
import com.example.creatorsettlement.domain.CancelRecord;
import com.example.creatorsettlement.repository.CancelRecordRepository;
import com.example.creatorsettlement.repository.CourseRepository;
import com.example.creatorsettlement.repository.CreatorRepository;
import com.example.creatorsettlement.repository.SaleRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired CreatorRepository creatorRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired SaleRecordRepository saleRecordRepository;
    @Autowired CancelRecordRepository cancelRecordRepository;

    private Creator creator;

    @BeforeEach
    void setUp() {
        creator = creatorRepository.save(Creator.builder().name("김민준").email("kim@example.com").build());
        Course course = courseRepository.save(Course.builder().title("Spring Boot 완성").creator(creator).build());

        // 2025-03 판매 2건
        SaleRecord sale1 = saleRecordRepository.save(SaleRecord.builder()
                .course(course).studentId(101L)
                .paymentAmount(new BigDecimal("50000"))
                .paymentAt(LocalDateTime.of(2025, 3, 5, 10, 0)).build());

        saleRecordRepository.save(SaleRecord.builder()
                .course(course).studentId(102L)
                .paymentAmount(new BigDecimal("35000"))
                .paymentAt(LocalDateTime.of(2025, 3, 12, 16, 0)).build());

        // 2025-03 취소 1건 (sale1 취소)
        sale1.cancel();
        cancelRecordRepository.save(CancelRecord.builder()
                .saleRecord(sale1)
                .refundAmount(new BigDecimal("50000"))
                .cancelledAt(LocalDateTime.of(2025, 3, 11, 15, 0)).build());
    }

    @Test
    @DisplayName("크리에이터 월별 정산 - 정상 응답")
    void getCreatorSettlement_success() throws Exception {
        mockMvc.perform(get("/api/settlements/creator/{id}", creator.getId())
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2025-03"))
                .andExpect(jsonPath("$.totalSalesAmount").value(85000))
                .andExpect(jsonPath("$.totalRefundAmount").value(50000))
                .andExpect(jsonPath("$.netSalesAmount").value(35000))
                .andExpect(jsonPath("$.commission").value(7000))
                .andExpect(jsonPath("$.settlementAmount").value(28000))
                .andExpect(jsonPath("$.saleCount").value(2))
                .andExpect(jsonPath("$.cancelCount").value(1));
    }

    @Test
    @DisplayName("크리에이터 월별 정산 - 존재하지 않는 크리에이터 404")
    void getCreatorSettlement_notFound() throws Exception {
        mockMvc.perform(get("/api/settlements/creator/9999")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("운영자 전체 정산 집계 - 정상 응답")
    void getAdminSettlement_success() throws Exception {
        mockMvc.perform(get("/api/settlements/admin")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorSummaries.length()").value(1))
                .andExpect(jsonPath("$.creatorSummaries[0].creatorName").value("김민준"))
                .andExpect(jsonPath("$.creatorSummaries[0].settlementAmount").value(28000))
                .andExpect(jsonPath("$.totalSettlementAmount").value(28000));
    }

    @Test
    @DisplayName("운영자 전체 정산 집계 - 해당 기간 데이터 없음")
    void getAdminSettlement_empty() throws Exception {
        mockMvc.perform(get("/api/settlements/admin")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorSummaries.length()").value(0))
                .andExpect(jsonPath("$.totalSettlementAmount").value(0));
    }
}
