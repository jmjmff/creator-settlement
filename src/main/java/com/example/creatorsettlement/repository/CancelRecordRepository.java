package com.example.creatorsettlement.repository;

import com.example.creatorsettlement.domain.CancelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, Long> {

    Optional<CancelRecord> findBySaleRecordId(Long saleRecordId);

    boolean existsBySaleRecordId(Long saleRecordId);

    @Query("SELECT c FROM CancelRecord c WHERE c.saleRecord.id IN :saleIds")
    List<CancelRecord> findBySaleRecordIdIn(@Param("saleIds") List<Long> saleIds);

    @Query("SELECT c FROM CancelRecord c JOIN FETCH c.saleRecord s JOIN FETCH s.course co JOIN FETCH co.creator cr WHERE cr.id = :creatorId AND c.cancelledAt >= :start AND c.cancelledAt < :end")
    List<CancelRecord> findByCreatorIdAndPeriod(@Param("creatorId") Long creatorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM CancelRecord c JOIN FETCH c.saleRecord s JOIN FETCH s.course co JOIN FETCH co.creator WHERE c.cancelledAt >= :start AND c.cancelledAt < :end")
    List<CancelRecord> findByCancelledAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
