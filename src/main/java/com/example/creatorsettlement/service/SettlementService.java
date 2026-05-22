package com.example.creatorsettlement.service;

import com.example.creatorsettlement.domain.CancelRecord;
import com.example.creatorsettlement.domain.Creator;
import com.example.creatorsettlement.domain.SaleRecord;
import com.example.creatorsettlement.dto.response.AdminSettlementResponse;
import com.example.creatorsettlement.dto.response.CreatorSettlementResponse;
import com.example.creatorsettlement.exception.NotFoundException;
import com.example.creatorsettlement.repository.CancelRecordRepository;
import com.example.creatorsettlement.repository.CreatorRepository;
import com.example.creatorsettlement.repository.SaleRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.20");

    private final CreatorRepository creatorRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;

    public CreatorSettlementResponse getCreatorSettlement(Long creatorId, String yearMonth) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("크리에이터를 찾을 수 없습니다. id=" + creatorId));

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<SaleRecord> sales = saleRecordRepository.findByCreatorIdAndPeriod(creatorId, start, end);
        List<CancelRecord> cancels = cancelRecordRepository.findByCreatorIdAndPeriod(creatorId, start, end);

        BigDecimal totalSales = sum(sales.stream().map(SaleRecord::getPaymentAmount).collect(Collectors.toList()));
        BigDecimal totalRefund = sum(cancels.stream().map(CancelRecord::getRefundAmount).collect(Collectors.toList()));
        BigDecimal netSales = totalSales.subtract(totalRefund);
        BigDecimal commission = netSales.multiply(COMMISSION_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal settlementAmount = netSales.subtract(commission);

        return CreatorSettlementResponse.builder()
                .creatorId(creator.getId())
                .creatorName(creator.getName())
                .yearMonth(yearMonth)
                .totalSalesAmount(totalSales)
                .totalRefundAmount(totalRefund)
                .netSalesAmount(netSales)
                .commission(commission)
                .settlementAmount(settlementAmount)
                .saleCount(sales.size())
                .cancelCount(cancels.size())
                .build();
    }

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
                    BigDecimal commission = netSales.multiply(COMMISSION_RATE).setScale(0, RoundingMode.HALF_UP);
                    BigDecimal settlementAmount = netSales.subtract(commission);

                    return AdminSettlementResponse.CreatorSummary.builder()
                            .creatorId(creator.getId())
                            .creatorName(creator.getName())
                            .totalSalesAmount(totalSales)
                            .totalRefundAmount(totalRefund)
                            .netSalesAmount(netSales)
                            .settlementAmount(settlementAmount)
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

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
