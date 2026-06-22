package com.innowise.paymentservice.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    private Long orderId;

    private Long userId;

    private BigDecimal paymentAmount;
}
