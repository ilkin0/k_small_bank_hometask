package com.ilkinmehdiyev.kapitalsmallbankingrest.controller;

import static com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders.X_IDEMPOTENCY_KEY;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilkinmehdiyev.kapitalsmallbankingrest.config.TestLiquibaseConfig;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.initalizer.PostgresSQLEmbeddedContainer;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import com.ilkinmehdiyev.kapitalsmallbankingrest.security.JwtService;
import com.ilkinmehdiyev.kapitalsmallbankingrest.service.CustomerServiceImpl.CustomUserDetails;
import com.ilkinmehdiyev.kapitalsmallbankingrest.service.TransactionService;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {PostgresSQLEmbeddedContainer.Initializer.class})
@Import(TestLiquibaseConfig.class)
class TransactionControllerTest {

  @LocalServerPort private int port;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TransactionService transactionService;
  @Autowired private JwtService jwtService;

  private final UUID customerUid = UUID.fromString("019630c5-eccf-7b24-b814-a39c97c64b8b");
  private final UUID transactionUid = UUID.randomUUID();
  private final String baseUrl = "/api/v1/account/transactions";
  private String jwtToken;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    RestAssured.basePath = "";
    RestAssured.baseURI = "https://localhost";

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);

    RestAssured.config =
        RestAssured.config()
            .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails())
            .sslConfig(
                SSLConfig.sslConfig()
                    .allowAllHostnames()
                    .relaxedHTTPSValidation()
                    .keyStore("classpath:keystore.p12", "QTntL1pa4QrJcf5R")
                    .keystoreType("PKCS12"));

    generateJwtToken();
  }

  @Test
  @DisplayName("Should successfully process a TOP_UP transaction")
  void shouldProcessTopUpTransaction() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, new BigDecimal("100.00"), null);
    TransactionResponse mockResponse =
        new TransactionResponse(transactionUid, TransactionStatus.COMPLETED, Instant.now());

    when(transactionService.processTransaction(any(TransactionRequest.class), any(UUID.class)))
        .thenReturn(mockResponse);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtToken)
        .header(X_IDEMPOTENCY_KEY, idempotencyKey.toString())
        .body(objectMapper.writeValueAsString(request))
        .when()
        .post(baseUrl)
        .then()
        .log()
        .ifValidationFails()
        .statusCode(HttpStatus.OK.value())
        .body("status.code", equalTo(HttpStatus.OK.value()))
        .body("data.transactionUid", is(transactionUid.toString()))
        .body("data.status", equalTo("COMPLETED"));
  }

  @Test
  @DisplayName("Should successfully process a PURCHASE transaction")
  void shouldProcessPurchaseTransaction() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    TransactionRequest request =
        new TransactionRequest(TransactionType.PURCHASE, new BigDecimal("50.00"), null);
    TransactionResponse mockResponse =
        new TransactionResponse(transactionUid, TransactionStatus.COMPLETED, Instant.now());

    when(transactionService.processTransaction(any(TransactionRequest.class), any(UUID.class)))
        .thenReturn(mockResponse);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtToken)
        .header(X_IDEMPOTENCY_KEY, idempotencyKey.toString())
        .body(objectMapper.writeValueAsString(request))
        .when()
        .post(baseUrl)
        .then()
        .log()
        .ifValidationFails()
        .statusCode(HttpStatus.OK.value())
        .body("status.code", equalTo(HttpStatus.OK.value()))
        .body("data.transactionUid", notNullValue())
        .body("data.status", equalTo("COMPLETED"));
  }

  @Test
  @DisplayName("Should successfully process a PARTIAL_REFUND transaction")
  void shouldProcessPartialRefundTransaction() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    UUID referenceTransactionUid = UUID.randomUUID();

    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND, new BigDecimal("25.00"), referenceTransactionUid);

    TransactionResponse mockResponse =
        new TransactionResponse(transactionUid, TransactionStatus.REFUNDED, Instant.now());

    when(transactionService.processTransaction(any(TransactionRequest.class), any(UUID.class)))
        .thenReturn(mockResponse);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtToken)
        .header(X_IDEMPOTENCY_KEY, idempotencyKey.toString())
        .body(objectMapper.writeValueAsString(request))
        .when()
        .post(baseUrl)
        .then()
        .log()
        .ifValidationFails()
        .statusCode(HttpStatus.OK.value())
        .body("status.code", equalTo(HttpStatus.OK.value()))
        .body("data.transactionUid", notNullValue())
        .body("data.status", equalTo("REFUNDED"));
  }

  @Test
  @DisplayName("Should return 400 for missing idempotency key header")
  void shouldReturn400ForMissingIdempotencyKeyHeader() throws Exception {
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, new BigDecimal("100.00"), null);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtToken)
        .body(objectMapper.writeValueAsString(request))
        .when()
        .post(baseUrl)
        .then()
        .log()
        .ifValidationFails()
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  @DisplayName("Should return same response for duplicate idempotency key")
  void shouldReturnSameResponseForDuplicateIdempotencyKey() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, new BigDecimal("100.00"), null);
    TransactionResponse mockResponse =
        new TransactionResponse(transactionUid, TransactionStatus.COMPLETED, Instant.now());

    when(transactionService.processTransaction(any(TransactionRequest.class), any(UUID.class)))
        .thenReturn(mockResponse);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtToken)
        .header(X_IDEMPOTENCY_KEY, idempotencyKey.toString())
        .body(objectMapper.writeValueAsString(request))
        .when()
        .post(baseUrl)
        .then()
        .log()
        .ifValidationFails()
        .statusCode(HttpStatus.OK.value())
        .body("data.transactionUid", is(transactionUid.toString()));

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + jwtToken)
        .header(X_IDEMPOTENCY_KEY, idempotencyKey.toString())
        .body(objectMapper.writeValueAsString(request))
        .when()
        .post(baseUrl)
        .then()
        .log()
        .ifValidationFails()
        .statusCode(HttpStatus.OK.value())
        .body("data.transactionUid", is(transactionUid.toString()));
  }

  private void generateJwtToken() {
    CustomUserDetails userDetails =
        new CustomUserDetails(1L, customerUid, "password", "+994501234567");

    jwtToken = jwtService.generateAccessToken(userDetails);
  }
}
