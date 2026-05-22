package com.example.creatorsettlement.repository;

import com.example.creatorsettlement.domain.SaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, Long> {

    @Query("SELECT s FROM SaleRecord s JOIN FETCH s.course c JOIN FETCH c.creator")
    List<SaleRecord> findAllWithCourseAndCreator();

    @Query("SELECT s FROM SaleRecord s JOIN FETCH s.course c JOIN FETCH c.creator cr WHERE cr.id = :creatorId")
    List<SaleRecord> findByCreatorId(@Param("creatorId") Long creatorId);

    @Query("SELECT s FROM SaleRecord s JOIN FETCH s.course c JOIN FETCH c.creator WHERE s.paymentAt >= :start AND s.paymentAt < :end")
    List<SaleRecord> findByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM SaleRecord s JOIN FETCH s.course c JOIN FETCH c.creator cr WHERE cr.id = :creatorId AND s.paymentAt >= :start AND s.paymentAt < :end")
    List<SaleRecord> findByCreatorIdAndPeriod(@Param("creatorId") Long creatorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
