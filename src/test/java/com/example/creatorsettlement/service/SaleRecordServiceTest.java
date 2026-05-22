package com.example.creatorsettlement.service;

import com.example.creatorsettlement.domain.Course;
import com.example.creatorsettlement.domain.Creator;
import com.example.creatorsettlement.domain.SaleRecord;
import com.example.creatorsettlement.dto.request.CancelRecordCreateRequest;
import com.example.creatorsettlement.dto.request.SaleRecordCreateRequest;
import com.example.creatorsettlement.dto.response.SaleRecordResponse;
import com.example.creatorsettlement.exception.AlreadyCancelledException;
import com.example.creatorsettlement.exception.NotFoundException;
import com.example.creatorsettlement.repository.CancelRecordRepository;
import com.example.creatorsettlement.repository.CourseRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SaleRecordServiceTest {

    @InjectMocks
    private SaleRecordService saleRecordService;

    @Mock
    private SaleRecordRepository saleRecordRepository;
    @Mock
    private CancelRecordRepository cancelRecordRepository;
    @Mock
    private CourseRepository courseRepository;

    private Creator creator;
    private Course course;
    private SaleRecord saleRecord;

    @BeforeEach
    void setUp() {
        creator = Creator.builder().name("김민준").email("kim@example.com").build();
        course = Course.builder().title("Spring Boot 완성").creator(creator).build();
        saleRecord = SaleRecord.builder()
                .course(course)
                .studentId(101L)
                .paymentAmount(new BigDecimal("50000"))
                .paymentAt(LocalDateTime.of(2025, 3, 5, 10, 0))
                .build();
    }

    @Test
    @DisplayName("판매 내역 등록 - 정상")
    void createSaleRecord_success() {
        SaleRecordCreateRequest request = new SaleRecordCreateRequest();
        setField(request, "courseId", 1L);
        setField(request, "studentId", 101L);
        setField(request, "paymentAmount", new BigDecimal("50000"));
        setField(request, "paymentAt", LocalDateTime.of(2025, 3, 5, 10, 0));

        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(saleRecordRepository.save(any())).willReturn(saleRecord);

        SaleRecordResponse response = saleRecordService.createSaleRecord(request);

        assertThat(response.getPaymentAmount()).isEqualByComparingTo("50000");
        assertThat(response.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("판매 내역 등록 - 존재하지 않는 강의")
    void createSaleRecord_courseNotFound() {
        SaleRecordCreateRequest request = new SaleRecordCreateRequest();
        setField(request, "courseId", 99L);
        setField(request, "studentId", 101L);
        setField(request, "paymentAmount", new BigDecimal("50000"));
        setField(request, "paymentAt", LocalDateTime.of(2025, 3, 5, 10, 0));

        given(courseRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> saleRecordService.createSaleRecord(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("강의를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("취소 내역 등록 - 정상")
    void createCancelRecord_success() {
        CancelRecordCreateRequest request = new CancelRecordCreateRequest();
        setField(request, "saleRecordId", 1L);
        setField(request, "refundAmount", new BigDecimal("50000"));
        setField(request, "cancelledAt", LocalDateTime.of(2025, 3, 11, 15, 0));

        given(saleRecordRepository.findById(1L)).willReturn(Optional.of(saleRecord));
        given(cancelRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        SaleRecordResponse response = saleRecordService.createCancelRecord(request);

        assertThat(response.isCancelled()).isTrue();
        assertThat(response.getCancelInfo()).isNotNull();
        assertThat(response.getCancelInfo().getRefundAmount()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("취소 내역 등록 - 이미 취소된 판매 내역")
    void createCancelRecord_alreadyCancelled() {
        saleRecord.cancel();

        CancelRecordCreateRequest request = new CancelRecordCreateRequest();
        setField(request, "saleRecordId", 1L);
        setField(request, "refundAmount", new BigDecimal("50000"));
        setField(request, "cancelledAt", LocalDateTime.of(2025, 3, 11, 15, 0));

        given(saleRecordRepository.findById(1L)).willReturn(Optional.of(saleRecord));

        assertThatThrownBy(() -> saleRecordService.createCancelRecord(request))
                .isInstanceOf(AlreadyCancelledException.class)
                .hasMessageContaining("이미 취소된");
    }

    @Test
    @DisplayName("취소 내역 등록 - 존재하지 않는 판매 내역")
    void createCancelRecord_saleNotFound() {
        CancelRecordCreateRequest request = new CancelRecordCreateRequest();
        setField(request, "saleRecordId", 99L);
        setField(request, "refundAmount", new BigDecimal("50000"));
        setField(request, "cancelledAt", LocalDateTime.of(2025, 3, 11, 15, 0));

        given(saleRecordRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> saleRecordService.createCancelRecord(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("판매 내역을 찾을 수 없습니다");
    }

    // Request DTO는 @NoArgsConstructor + private 필드라 리플렉션으로 세팅
    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
