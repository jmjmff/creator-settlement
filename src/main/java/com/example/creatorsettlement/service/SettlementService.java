package com.example.creatorsettlement.service;

import com.example.creatorsettlement.domain.*;
import com.example.creatorsettlement.dto.response.AdminSettlementResponse;
import com.example.creatorsettlement.dto.response.CreatorSettlementResponse;
import com.example.creatorsettlement.dto.response.SettlementResponse;
import com.example.creatorsettlement.exception.DuplicateSettlementException;
import com.example.creatorsettlement.exception.NotFoundException;
import com.example.creatorsettlement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    // 수수료율 이력 테이블에 해당 기간 데이터가 없을 때 사용하는 기본값
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.20");

    private final CreatorRepository creatorRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final SettlementRepository settlementRepository;
    private final CommissionRateHistoryRepository commissionRateHistoryRepository;

    // ─────────────────────────────────────────────────────────────────
    // [기존] 실시간 계산 조회 API
    // ─────────────────────────────────────────────────────────────────

    /**
     * 크리에이터별 월 정산 실시간 계산.
     * SaleRecord는 paymentAt 기준, CancelRecord는 cancelledAt 기준으로 각각 집계.
     * 수수료율은 해당 월 1일 기준 이력 테이블에서 조회.
     */
    public CreatorSettlementResponse getCreatorSettlement(Long creatorId, String yearMonth) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("크리에이터를 찾을 수 없습니다. id=" + creatorId));

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<SaleRecord> sales = saleRecordRepository.findByCreatorIdAndPeriod(creatorId, start, end);
        List<CancelRecord> cancels = cancelRecordRepository.findByCreatorIdAndPeriod(creatorId, start, end);

        BigDecimal rate = resolveRate(ym);
        BigDecimal totalSales = sum(sales.stream().map(SaleRecord::getPaymentAmount).collect(Collectors.toList()));
        BigDecimal totalRefund = sum(cancels.stream().map(CancelRecord::getRefundAmount).collect(Collectors.toList()));
        BigDecimal netSales = totalSales.subtract(totalRefund);
        BigDecimal commission = netSales.multiply(rate).setScale(0, RoundingMode.HALF_UP);

        return CreatorSettlementResponse.builder()
                .creatorId(creator.getId())
                .creatorName(creator.getName())
                .yearMonth(yearMonth)
                .totalSalesAmount(totalSales)
                .totalRefundAmount(totalRefund)
                .netSalesAmount(netSales)
                .commission(commission)
                .settlementAmount(netSales.subtract(commission))
                .saleCount(sales.size())
                .cancelCount(cancels.size())
                .build();
    }

    /**
     * 어드민 기간별 전체 정산 실시간 계산.
     * 기간 내 모든 크리에이터를 집계하여 반환.
     */
    public AdminSettlementResponse getAdminSettlement(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<SaleRecord> allSales = saleRecordRepository.findByPeriod(start, end);
        List<CancelRecord> allCancels = cancelRecordRepository.findByCancelledAtBetween(start, end);

        Map<Creator, List<SaleRecord>> salesByCreator = allSales.stream()
                .collect(Collectors.groupingBy(s -> s.getCourse().getCreator()));
        Map<Creator, List<CancelRecord>> cancelsByCreator = allCancels.stream()
                .collect(Collectors.groupingBy(c -> c.getSaleRecord().getCourse().getCreator()));

        Set<Creator> allCreators = new TreeSet<>(Comparator.comparing(Creator::getId));
        allCreators.addAll(salesByCreator.keySet());
        allCreators.addAll(cancelsByCreator.keySet());

        List<AdminSettlementResponse.CreatorSummary> summaries = allCreators.stream()
                .map(creator -> {
                    List<SaleRecord> sales = salesByCreator.getOrDefault(creator, List.of());
                    List<CancelRecord> cancels = cancelsByCreator.getOrDefault(creator, List.of());
                    BigDecimal totalSales = sum(sales.stream().map(SaleRecord::getPaymentAmount).collect(Collectors.toList()));
                    BigDecimal totalRefund = sum(cancels.stream().map(CancelRecord::getRefundAmount).collect(Collectors.toList()));
                    BigDecimal netSales = totalSales.subtract(totalRefund);
                    BigDecimal commission = netSales.multiply(DEFAULT_COMMISSION_RATE).setScale(0, RoundingMode.HALF_UP);
                    return AdminSettlementResponse.CreatorSummary.builder()
                            .creatorId(creator.getId())
                            .creatorName(creator.getName())
                            .totalSalesAmount(totalSales)
                            .totalRefundAmount(totalRefund)
                            .netSalesAmount(netSales)
                            .settlementAmount(netSales.subtract(commission))
                            .saleCount(sales.size())
                            .cancelCount(cancels.size())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = summaries.stream()
                .map(AdminSettlementResponse.CreatorSummary::getSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminSettlementResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .creatorSummaries(summaries)
                .totalSettlementAmount(total)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // [신규] 정산 확정 상태 관리: PENDING → CONFIRMED → PAID
    // ─────────────────────────────────────────────────────────────────

    /**
     * 정산 생성 (PENDING 상태로 저장).
     * - 동일 creatorId + yearMonth 조합이 이미 존재하면 409 예외
     * - 해당 월의 수수료율을 이력 테이블에서 조회하여 스냅샷으로 저장
     *   (과거 정산은 생성 당시 수수료율이 영구 보존됨)
     */
    @Transactional
    public SettlementResponse createSettlement(Long creatorId, String yearMonth) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("크리에이터를 찾을 수 없습니다. id=" + creatorId));

        if (settlementRepository.existsByCreatorIdAndYearMonth(creatorId, yearMonth)) {
            throw new DuplicateSettlementException(
                    "이미 정산이 생성된 기간입니다. creatorId=" + creatorId + ", yearMonth=" + yearMonth);
        }

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<SaleRecord> sales = saleRecordRepository.findByCreatorIdAndPeriod(creatorId, start, end);
        List<CancelRecord> cancels = cancelRecordRepository.findByCreatorIdAndPeriod(creatorId, start, end);

        BigDecimal rate = resolveRate(ym);
        BigDecimal totalSales = sum(sales.stream().map(SaleRecord::getPaymentAmount).collect(Collectors.toList()));
        BigDecimal totalRefund = sum(cancels.stream().map(CancelRecord::getRefundAmount).collect(Collectors.toList()));
        BigDecimal netSales = totalSales.subtract(totalRefund);
        BigDecimal commission = netSales.multiply(rate).setScale(0, RoundingMode.HALF_UP);

        Settlement settlement = Settlement.builder()
                .creator(creator)
                .yearMonth(yearMonth)
                .commissionRate(rate)
                .totalSalesAmount(totalSales)
                .totalRefundAmount(totalRefund)
                .netSalesAmount(netSales)
                .commission(commission)
                .settlementAmount(netSales.subtract(commission))
                .saleCount(sales.size())
                .cancelCount(cancels.size())
                .build();

        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    /**
     * PENDING → CONFIRMED.
     * 이미 CONFIRMED/PAID 상태이면 InvalidSettlementStatusException 발생.
     */
    @Transactional
    public SettlementResponse confirmSettlement(Long settlementId) {
        Settlement settlement = findSettlement(settlementId);
        settlement.confirm();
        return SettlementResponse.from(settlement);
    }

    /**
     * CONFIRMED → PAID.
     * PENDING 또는 이미 PAID 상태이면 InvalidSettlementStatusException 발생.
     */
    @Transactional
    public SettlementResponse paySettlement(Long settlementId) {
        Settlement settlement = findSettlement(settlementId);
        settlement.pay();
        return SettlementResponse.from(settlement);
    }

    /** 정산 단건 조회 */
    public SettlementResponse getSettlement(Long settlementId) {
        return SettlementResponse.from(findSettlement(settlementId));
    }

    // ─────────────────────────────────────────────────────────────────
    // [신규] CSV 다운로드
    // ─────────────────────────────────────────────────────────────────

    /**
     * 전체 정산 내역을 CSV 바이트로 반환.
     * UTF-8 BOM(﻿) 포함 → Excel에서 한글 깨짐 방지.
     */
    public byte[] exportCsv() {
        List<Settlement> settlements = settlementRepository.findAllWithCreator();

        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // Excel UTF-8 BOM
        sb.append("정산ID,크리에이터ID,크리에이터명,정산월,수수료율,총매출,총환불,순매출,수수료,정산액,판매건수,취소건수,상태,확정일시,지급일시\n");

        for (Settlement s : settlements) {
            sb.append(s.getId()).append(',')
              .append(s.getCreator().getId()).append(',')
              .append(s.getCreator().getName()).append(',')
              .append(s.getYearMonth()).append(',')
              .append(s.getCommissionRate()).append(',')
              .append(s.getTotalSalesAmount()).append(',')
              .append(s.getTotalRefundAmount()).append(',')
              .append(s.getNetSalesAmount()).append(',')
              .append(s.getCommission()).append(',')
              .append(s.getSettlementAmount()).append(',')
              .append(s.getSaleCount()).append(',')
              .append(s.getCancelCount()).append(',')
              .append(s.getStatus()).append(',')
              .append(s.getConfirmedAt() != null ? s.getConfirmedAt() : "").append(',')
              .append(s.getPaidAt() != null ? s.getPaidAt() : "").append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────────────────────

    private Settlement findSettlement(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("정산을 찾을 수 없습니다. id=" + id));
    }

    /**
     * 해당 월 1일 기준으로 적용 중인 수수료율 조회.
     * commission_rate_histories 테이블에서 appliedFrom <= 1일 <= appliedTo 조건으로 검색.
     * 이력 없으면 DEFAULT_COMMISSION_RATE(20%) 사용.
     */
    private BigDecimal resolveRate(YearMonth ym) {
        return commissionRateHistoryRepository.findActiveRateAt(ym.atDay(1))
                .stream()
                .findFirst()
                .map(CommissionRateHistory::getRate)
                .orElse(DEFAULT_COMMISSION_RATE);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
