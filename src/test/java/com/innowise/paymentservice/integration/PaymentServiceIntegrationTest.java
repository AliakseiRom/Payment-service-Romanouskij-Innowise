package com.innowise.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.dto.request.CreatePaymentRequest;
import com.innowise.paymentservice.dto.response.PaymentResponse;
import com.innowise.paymentservice.kafka.event.CreatePaymentEvent;
import com.innowise.paymentservice.model.Payment;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import com.innowise.paymentservice.util.Status;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PaymentServiceIntegrationTest {

    private static final String TOPIC = "create-payment-events-test";

    private static final WireMockServer wireMockServer = new WireMockServer(0);

    static {
        wireMockServer.start();
    }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("payment_service_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.change-log", () -> "classpath:/db/changelog/db.changelog-master.yaml");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");

        registry.add("kafka.topics.create-payment", () -> TOPIC);

        registry.add("random-number-service.url", () -> wireMockServer.baseUrl() + "/api/v1.0/random");
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        wireMockServer.resetAll();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void createPayment_ShouldSaveSuccessPaymentAndSendKafkaEvent_WhenExternalApiReturnsEvenNumber() throws Exception {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1.0/random"))
                .withQueryParam("min", equalTo("1"))
                .withQueryParam("max", equalTo("100"))
                .withQueryParam("count", equalTo("1"))
                .willReturn(okJson("[2]")));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(10L);
        request.setUserId(20L);
        request.setPaymentAmount(new BigDecimal("150.75"));

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(response.getPaymentAmount()).isEqualByComparingTo("150.75");

        Payment savedPayment = paymentRepository.findById(response.getId()).orElseThrow();

        assertThat(savedPayment.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(savedPayment.getPaymentAmount()).isEqualByComparingTo("150.75");
        assertThat(savedPayment.getTimestamp()).isNotNull();

        CreatePaymentEvent event = consumePaymentEvent(response.getId());

        assertThat(event.getEventType()).isEqualTo("CREATE_PAYMENT");
        assertThat(event.getPaymentId()).isEqualTo(response.getId());
        assertThat(event.getOrderId()).isEqualTo(10L);
        assertThat(event.getUserId()).isEqualTo(20L);
        assertThat(event.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(event.getPaymentAmount()).isEqualByComparingTo("150.75");

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1.0/random")));
    }

    @Test
    void createPayment_ShouldSaveFailedPaymentAndSendKafkaEvent_WhenExternalApiReturnsOddNumber() throws Exception {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1.0/random"))
                .withQueryParam("min", equalTo("1"))
                .withQueryParam("max", equalTo("100"))
                .withQueryParam("count", equalTo("1"))
                .willReturn(okJson("[3]")));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(11L);
        request.setUserId(21L);
        request.setPaymentAmount(new BigDecimal("99.99"));

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Status.FAILED);

        Payment savedPayment = paymentRepository.findById(response.getId()).orElseThrow();

        assertThat(savedPayment.getStatus()).isEqualTo(Status.FAILED);
        assertThat(savedPayment.getPaymentAmount()).isEqualByComparingTo("99.99");

        CreatePaymentEvent event = consumePaymentEvent(response.getId());

        assertThat(event.getEventType()).isEqualTo("CREATE_PAYMENT");
        assertThat(event.getPaymentId()).isEqualTo(response.getId());
        assertThat(event.getOrderId()).isEqualTo(11L);
        assertThat(event.getUserId()).isEqualTo(21L);
        assertThat(event.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(event.getPaymentAmount()).isEqualByComparingTo("99.99");
    }

    private CreatePaymentEvent consumePaymentEvent(Long expectedPaymentId) throws Exception {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(TOPIC));

            long deadline = System.currentTimeMillis() + 10_000;

            while (System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));

                for (var record : records) {
                    CreatePaymentEvent event = objectMapper.readValue(
                            record.value(),
                            CreatePaymentEvent.class
                    );

                    if (expectedPaymentId.equals(event.getPaymentId())) {
                        return event;
                    }
                }
            }
        }

        throw new IllegalStateException("Kafka event was not received for paymentId=" + expectedPaymentId);
    }
}