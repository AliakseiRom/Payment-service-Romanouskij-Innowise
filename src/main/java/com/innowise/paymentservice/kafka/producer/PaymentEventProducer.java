package com.innowise.paymentservice.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.kafka.event.CreatePaymentEvent;
import com.innowise.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.create-payment}")
    private String createPaymentTopic;

    public void sendCreatePaymentEvent(Payment payment) {
        CreatePaymentEvent event = new CreatePaymentEvent(
                "CREATE_PAYMENT",
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus().name(),
                payment.getPaymentAmount(),
                payment.getTimestamp()
        );

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    createPaymentTopic,
                    payment.getOrderId().toString(),
                    message
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize CREATE_PAYMENT event", e);
        }
    }
}
