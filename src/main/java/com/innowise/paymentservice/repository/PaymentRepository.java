package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.util.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("""
            SELECT p
            FROM Payment p
            WHERE (:userId IS NULL OR p.userId = :userId)
              AND (:orderId IS NULL OR p.orderId = :orderId)
              AND (:status IS NULL OR p.status = :status)
            """)
    List<Payment> findByFilters(
            @Param("userId") Long userId,
            @Param("orderId") Long orderId,
            @Param("status") Status status
    );

    @Query("""
            SELECT COALESCE(SUM(p.paymentAmount), 0)
            FROM Payment p
            WHERE p.userId = :userId
              AND p.timestamp BETWEEN :from AND :to
              AND p.status = 'SUCCESS'
            """)
    BigDecimal getTotalSumForUserByDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT COALESCE(SUM(p.paymentAmount), 0)
            FROM Payment p
            WHERE p.timestamp BETWEEN :from AND :to
              AND p.status = 'SUCCESS'
            """)
    BigDecimal getTotalSumForAllUsersByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
