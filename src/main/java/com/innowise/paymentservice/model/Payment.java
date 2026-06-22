package com.innowise.paymentservice.model;

import com.innowise.paymentservice.util.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "payment_timestamp")
    private LocalDateTime timestamp;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @PrePersist
    public void prePersist() {
        timestamp = LocalDateTime.now();
    }
}
