package com.innowise.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentSumResponse {

    private BigDecimal totalSum;
}
