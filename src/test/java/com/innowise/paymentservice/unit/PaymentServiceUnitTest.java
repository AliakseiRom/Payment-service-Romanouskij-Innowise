package com.innowise.paymentservice.unit;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.dto.response.PaymentSumResponse;
import com.innowise.paymentservice.kafka.producer.PaymentEventProducer;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.util.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_ShouldCreatePaymentWithSuccessStatus_WhenRandomNumberIsEven() {
        CreatePaymentRequest request = createRequest();

        Payment payment = createPayment(null, null);
        PaymentResponse response = createResponse(1L, Status.SUCCESS);

        when(paymentMapper.toPayment(request)).thenReturn(payment);
        when(randomNumberClient.getRandomNumber()).thenReturn(2);
        when(paymentRepository.save(payment)).thenAnswer(invocation -> {
            Payment savedPayment = invocation.getArgument(0);
            savedPayment.setId(1L);
            return savedPayment;
        });
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(Status.SUCCESS);

        verify(paymentRepository).save(payment);
        verify(paymentEventProducer).sendCreatePaymentEvent(payment);
    }

    @Test
    void createPayment_ShouldCreatePaymentWithFailedStatus_WhenRandomNumberIsOdd() {
        CreatePaymentRequest request = createRequest();

        Payment payment = createPayment(null, null);
        PaymentResponse response = createResponse(1L, Status.FAILED);

        when(paymentMapper.toPayment(request)).thenReturn(payment);
        when(randomNumberClient.getRandomNumber()).thenReturn(3);
        when(paymentRepository.save(payment)).thenAnswer(invocation -> {
            Payment savedPayment = invocation.getArgument(0);
            savedPayment.setId(1L);
            return savedPayment;
        });
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Status.FAILED);
        assertThat(payment.getStatus()).isEqualTo(Status.FAILED);

        verify(paymentRepository).save(payment);
        verify(paymentEventProducer).sendCreatePaymentEvent(payment);
    }

    @Test
    void getPaymentById_ShouldReturnPayment_WhenPaymentExists() {
        Payment payment = createPayment(1L, Status.SUCCESS);
        PaymentResponse response = createResponse(1L, Status.SUCCESS);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.getPaymentById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);

        verify(paymentRepository).findById(1L);
    }

    @Test
    void getPaymentById_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment not found");

        verify(paymentRepository).findById(1L);
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void getPayments_ShouldReturnFilteredPayments() {
        Payment firstPayment = createPayment(1L, Status.SUCCESS);
        Payment secondPayment = createPayment(2L, Status.SUCCESS);

        PaymentResponse firstResponse = createResponse(1L, Status.SUCCESS);
        PaymentResponse secondResponse = createResponse(2L, Status.SUCCESS);

        when(paymentRepository.findByFilters(1L, null, Status.SUCCESS))
                .thenReturn(List.of(firstPayment, secondPayment));
        when(paymentMapper.toPaymentResponse(firstPayment)).thenReturn(firstResponse);
        when(paymentMapper.toPaymentResponse(secondPayment)).thenReturn(secondResponse);

        List<PaymentResponse> result = paymentService.getPayments(1L, null, Status.SUCCESS);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PaymentResponse::getId)
                .containsExactly(1L, 2L);

        verify(paymentRepository).findByFilters(1L, null, Status.SUCCESS);
    }

    @Test
    void getTotalSumForCurrentUser_ShouldReturnTotalSum() {
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 1, 23, 59);

        when(paymentRepository.getTotalSumForUserByDateRange(1L, from, to))
                .thenReturn(new BigDecimal("300.50"));

        PaymentSumResponse result = paymentService.getTotalSumForCurrentUser(1L, from, to);

        assertThat(result.getTotalSum()).isEqualByComparingTo("300.50");

        verify(paymentRepository).getTotalSumForUserByDateRange(1L, from, to);
    }

    @Test
    void getTotalSumForAllUsers_ShouldReturnTotalSum() {
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 1, 23, 59);

        when(paymentRepository.getTotalSumForAllUsersByDateRange(from, to))
                .thenReturn(new BigDecimal("600.50"));

        PaymentSumResponse result = paymentService.getTotalSumForAllUsers(from, to);

        assertThat(result.getTotalSum()).isEqualByComparingTo("600.50");

        verify(paymentRepository).getTotalSumForAllUsersByDateRange(from, to);
    }

    @Test
    void updatePaymentStatus_ShouldUpdateStatus_WhenPaymentExists() {
        Payment payment = createPayment(1L, Status.FAILED);
        PaymentResponse response = createResponse(1L, Status.SUCCESS);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.updatePaymentStatus(1L, Status.SUCCESS);

        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(Status.SUCCESS);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository).save(payment);
    }

    @Test
    void updatePaymentStatus_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, Status.SUCCESS))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment not found");

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void deletePayment_ShouldDeletePayment_WhenPaymentExists() {
        when(paymentRepository.existsById(1L)).thenReturn(true);

        paymentService.deletePayment(1L);

        verify(paymentRepository).existsById(1L);
        verify(paymentRepository).deleteById(1L);
    }

    @Test
    void deletePayment_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.deletePayment(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment not found");

        verify(paymentRepository).existsById(1L);
        verify(paymentRepository, never()).deleteById(anyLong());
    }

    private CreatePaymentRequest createRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(1L);
        request.setUserId(1L);
        request.setPaymentAmount(new BigDecimal("100.50"));
        return request;
    }

    private Payment createPayment(Long id, Status status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setOrderId(1L);
        payment.setUserId(1L);
        payment.setStatus(status);
        payment.setPaymentAmount(new BigDecimal("100.50"));
        payment.setTimestamp(LocalDateTime.of(2026, 7, 1, 12, 0));
        return payment;
    }

    private PaymentResponse createResponse(Long id, Status status) {
        PaymentResponse response = new PaymentResponse();
        response.setId(id);
        response.setOrderId(1L);
        response.setUserId(1L);
        response.setStatus(status);
        response.setPaymentAmount(new BigDecimal("100.50"));
        response.setTimestamp(LocalDateTime.of(2026, 7, 1, 12, 0));
        return response;
    }
}
