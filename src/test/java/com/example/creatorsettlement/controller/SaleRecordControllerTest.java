package com.example.creatorsettlement.controller;

import com.example.creatorsettlement.domain.Course;
import com.example.creatorsettlement.domain.Creator;
import com.example.creatorsettlement.repository.CourseRepository;
import com.example.creatorsettlement.repository.CreatorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SaleRecordControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CreatorRepository creatorRepository;
    @Autowired CourseRepository courseRepository;

    private Long courseId;

    @BeforeEach
    void setUp() {
        Creator creator = creatorRepository.save(Creator.builder().name("김민준").email("kim@example.com").build());
        Course course = courseRepository.save(Course.builder().title("Spring Boot 완성").creator(creator).build());
        courseId = course.getId();
    }

    @Test
    @DisplayName("판매 내역 등록 - 201 Created")
    void createSaleRecord_created() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "courseId", courseId,
                "studentId", 101,
                "paymentAmount", 50000,
                "paymentAt", "2025-03-05T10:00:00"
        ));

        mockMvc.perform(post("/api/sale-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentAmount").value(50000))
                .andExpect(jsonPath("$.cancelled").value(false));
    }

    @Test
    @DisplayName("판매 내역 등록 - 존재하지 않는 강의 404")
    void createSaleRecord_courseNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "courseId", 9999,
                "studentId", 101,
                "paymentAmount", 50000,
                "paymentAt", "2025-03-05T10:00:00"
        ));

        mockMvc.perform(post("/api/sale-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("판매 내역 등록 - 필수 필드 누락 400")
    void createSaleRecord_validationFail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("courseId", courseId));

        mockMvc.perform(post("/api/sale-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("취소 내역 등록 후 재취소 시 409 Conflict")
    void createCancelRecord_alreadyCancelled() throws Exception {
        // 판매 등록
        String saleBody = objectMapper.writeValueAsString(Map.of(
                "courseId", courseId,
                "studentId", 101,
                "paymentAmount", 50000,
                "paymentAt", "2025-03-05T10:00:00"
        ));
        String saleResult = mockMvc.perform(post("/api/sale-records")
                        .contentType(MediaType.APPLICATION_JSON).content(saleBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long saleId = objectMapper.readTree(saleResult).get("id").asLong();

        // 첫 번째 취소
        String cancelBody = objectMapper.writeValueAsString(Map.of(
                "saleRecordId", saleId,
                "refundAmount", 50000,
                "cancelledAt", "2025-03-11T15:00:00"
        ));
        mockMvc.perform(post("/api/cancel-records")
                        .contentType(MediaType.APPLICATION_JSON).content(cancelBody))
                .andExpect(status().isCreated());

        // 두 번째 취소 시도 → 409
        mockMvc.perform(post("/api/cancel-records")
                        .contentType(MediaType.APPLICATION_JSON).content(cancelBody))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("판매 내역 목록 조회 - 전체")
    void getSaleRecords_all() throws Exception {
        // 판매 2건 등록
        for (int i = 0; i < 2; i++) {
            String body = objectMapper.writeValueAsString(Map.of(
                    "courseId", courseId,
                    "studentId", 100 + i,
                    "paymentAmount", 50000,
                    "paymentAt", "2025-03-05T10:00:00"
            ));
            mockMvc.perform(post("/api/sale-records")
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        mockMvc.perform(get("/api/sale-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
