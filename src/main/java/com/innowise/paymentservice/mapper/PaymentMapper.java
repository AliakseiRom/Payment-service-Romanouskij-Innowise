package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.util.Status;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {Status.class})
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toPayment(CreatePaymentRequest createPaymentRequest);

    PaymentResponse toPaymentResponse(Payment payment);
}
