package com.example.creatorsettlement.service;

import com.example.creatorsettlement.domain.CancelRecord;
import com.example.creatorsettlement.domain.Course;
import com.example.creatorsettlement.domain.SaleRecord;
import com.example.creatorsettlement.dto.request.CancelRecordCreateRequest;
import com.example.creatorsettlement.dto.request.SaleRecordCreateRequest;
import com.example.creatorsettlement.dto.response.SaleRecordResponse;
import com.example.creatorsettlement.exception.AlreadyCancelledException;
import com.example.creatorsettlement.exception.NotFoundException;
import com.example.creatorsettlement.repository.CancelRecordRepository;
import com.example.creatorsettlement.repository.CourseRepository;
import com.example.creatorsettlement.repository.SaleRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleRecordService {

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public SaleRecordResponse createSaleRecord(SaleRecordCreateRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new NotFoundException("강의를 찾을 수 없습니다. id=" + request.getCourseId()));

        SaleRecord saleRecord = SaleRecord.builder()
                .course(course)
                .studentId(request.getStudentId())
                .paymentAmount(request.getPaymentAmount())
                .paymentAt(request.getPaymentAt())
                .build();

        return SaleRecordResponse.of(saleRecordRepository.save(saleRecord), null);
    }

    @Transactional
    public SaleRecordResponse createCancelRecord(CancelRecordCreateRequest request) {
        SaleRecord saleRecord = saleRecordRepository.findById(request.getSaleRecordId())
                .orElseThrow(() -> new NotFoundException("판매 내역을 찾을 수 없습니다. id=" + request.getSaleRecordId()));

        if (saleRecord.isCancelled()) {
            throw new AlreadyCancelledException("이미 취소된 판매 내역입니다. id=" + request.getSaleRecordId());
        }

        saleRecord.cancel();

        CancelRecord cancelRecord = CancelRecord.builder()
                .saleRecord(saleRecord)
                .refundAmount(request.getRefundAmount())
                .cancelledAt(request.getCancelledAt())
                .build();

        cancelRecordRepository.save(cancelRecord);
        return SaleRecordResponse.of(saleRecord, cancelRecord);
    }

    public List<SaleRecordResponse> getSaleRecords(Long creatorId, LocalDate startDate, LocalDate endDate) {
        List<SaleRecord> records = queryRecords(creatorId, startDate, endDate);

        List<Long> saleIds = records.stream().map(SaleRecord::getId).collect(Collectors.toList());
        Map<Long, CancelRecord> cancelMap = saleIds.isEmpty() ? Map.of() :
                cancelRecordRepository.findBySaleRecordIdIn(saleIds).stream()
                        .collect(Collectors.toMap(c -> c.getSaleRecord().getId(), c -> c));

        return records.stream()
                .map(s -> SaleRecordResponse.of(s, cancelMap.get(s.getId())))
                .collect(Collectors.toList());
    }

    private List<SaleRecord> queryRecords(Long creatorId, LocalDate startDate, LocalDate endDate) {
        boolean hasCreator = creatorId != null;
        boolean hasPeriod = startDate != null && endDate != null;

        if (hasCreator && hasPeriod) {
            return saleRecordRepository.findByCreatorIdAndPeriod(
                    creatorId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
        if (hasCreator) {
            return saleRecordRepository.findByCreatorId(creatorId);
        }
        if (hasPeriod) {
            return saleRecordRepository.findByPeriod(
                    startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
        return saleRecordRepository.findAllWithCourseAndCreator();
    }
}
