package com.innowise.paymentservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentEvent {

    private String eventType;

    private Long paymentId;

    private Long orderId;

    private Long userId;

    private String paymentStatus;

    private BigDecimal paymentAmount;

    private LocalDateTime timestamp;
}
