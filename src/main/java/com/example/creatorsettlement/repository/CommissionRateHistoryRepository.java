package com.example.creatorsettlement.repository;

import com.example.creatorsettlement.domain.CommissionRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CommissionRateHistoryRepository extends JpaRepository<CommissionRateHistory, Long> {

    // 특정 날짜에 적용 중인 수수료율 조회 (appliedFrom <= date <= appliedTo, appliedTo null이면 현재 적용 중)
    @Query("SELECT c FROM CommissionRateHistory c WHERE c.appliedFrom <= :date AND (c.appliedTo IS NULL OR c.appliedTo >= :date) ORDER BY c.appliedFrom DESC")
    List<CommissionRateHistory> findActiveRateAt(@Param("date") LocalDate date);
}
