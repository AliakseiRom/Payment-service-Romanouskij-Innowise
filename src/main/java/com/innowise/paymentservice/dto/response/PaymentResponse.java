package com.innowise.paymentservice.dto.response;

import com.innowise.paymentservice.util.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {

    private Long id;

    private Long orderId;

    private Long userId;

    private Status status;

    private LocalDateTime timestamp;

    private BigDecimal paymentAmount;
}
