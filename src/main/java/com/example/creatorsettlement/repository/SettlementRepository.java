package com.example.creatorsettlement.repository;

import com.example.creatorsettlement.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByCreatorIdAndYearMonth(Long creatorId, String yearMonth);

    boolean existsByCreatorIdAndYearMonth(Long creatorId, String yearMonth);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.creator ORDER BY s.yearMonth DESC, s.creator.id ASC")
    List<Settlement> findAllWithCreator();
}
