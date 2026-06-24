package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.dto.response.PaymentSumResponse;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request
    ) {
        return new ResponseEntity<>(
                paymentService.createPayment(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id
    ) {
        return new ResponseEntity<>(
                paymentService.getPaymentById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Status status
    ) {
        return new ResponseEntity<>(
                paymentService.getPayments(userId, orderId, status),
                HttpStatus.OK
        );
    }

    @GetMapping("/sum/current-user")
    public ResponseEntity<PaymentSumResponse> getTotalSumForCurrentUser(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return new ResponseEntity<>(
                paymentService.getTotalSumForCurrentUser(userId, from, to),
                HttpStatus.OK
        );
    }

    @GetMapping("/sum/all-users")
    public ResponseEntity<PaymentSumResponse> getTotalSumForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return new ResponseEntity<>(
                paymentService.getTotalSumForAllUsers(from, to),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam Status status
    ) {
        return new ResponseEntity<>(
                paymentService.updatePaymentStatus(id, status),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long id
    ) {
        paymentService.deletePayment(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
