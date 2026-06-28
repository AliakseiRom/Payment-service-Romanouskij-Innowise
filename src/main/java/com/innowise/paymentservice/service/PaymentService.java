package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.dto.response.PaymentSumResponse;
import com.innowise.paymentservice.kafka.producer.PaymentEventProducer;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.util.Status;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final PaymentMapper paymentMapper;

    private final RandomNumberClient randomNumberClient;

    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = paymentMapper.toPayment(request);

        Integer randomNumber = randomNumberClient.getRandomNumber();

        if (randomNumber % 2 == 0) {
            payment.setStatus(Status.SUCCESS);
        } else {
            payment.setStatus(Status.FAILED);
        }

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventProducer.sendCreatePaymentEvent(savedPayment);

        return paymentMapper.toPaymentResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional
    public List<PaymentResponse> getPayments(Long userId, Long orderId, Status status) {
        return paymentRepository.findByFilters(userId, orderId, status)
                .stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    @Transactional
    public PaymentSumResponse getTotalSumForCurrentUser(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        BigDecimal totalSum = paymentRepository.getTotalSumForUserByDateRange(
                userId,
                from,
                to
        );

        return new PaymentSumResponse(totalSum);
    }

    @Transactional
    public PaymentSumResponse getTotalSumForAllUsers(
            LocalDateTime from,
            LocalDateTime to
    ) {
        BigDecimal totalSum = paymentRepository.getTotalSumForAllUsersByDateRange(
                from,
                to
        );

        return new PaymentSumResponse(totalSum);
    }

    @Transactional
    public PaymentResponse updatePaymentStatus(Long id, Status status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(status);

        return paymentMapper.toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Payment not found");
        }

        paymentRepository.deleteById(id);
    }
}
