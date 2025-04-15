package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.ilkinmehdiyev.kapitalsmallbankingrest.config.TestLiquibaseConfig;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransactionException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.initalizer.PostgresSQLEmbeddedContainer;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ContextConfiguration(initializers = {PostgresSQLEmbeddedContainer.Initializer.class})
@Import(TestLiquibaseConfig.class)
@Sql(scripts = "/sql/init-test-db.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class TransactionRepositoryITest {

  @Autowired private JdbcClient jdbcClient;
  @Autowired private CustomerRepository customerRepository;
  private TransactionRepository transactionRepository;

  private final UUID existingTransactionUid =
      UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
  private UUID nonExistingTransactionUid;
  private final UUID customerUid = UUID.fromString("019630c5-eccf-7b24-b814-a39c97c64b8b");
  private final Long customerId = 1L;

  @BeforeEach
  void setUp() {
    nonExistingTransactionUid = UUID.randomUUID();
    transactionRepository = new TransactionRepository(jdbcClient, customerRepository);
  }

  @Test
  @DisplayName("Should find transaction by UID when it exists")
  void shouldFindTransactionByUidWhenExists() {
    Optional<Transaction> result = transactionRepository.findByUid(existingTransactionUid);

    assertThat(result).isPresent();
    assertThat(result.get().uid()).isEqualTo(existingTransactionUid);
    assertThat(result.get().customerId()).isEqualTo(customerId);
    assertThat(result.get().type()).isEqualTo(TransactionType.TOP_UP);
    assertThat(result.get().amount()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  @DisplayName("Should return empty when transaction does not exist")
  void shouldReturnEmptyWhenTransactionDoesNotExist() {
    Optional<Transaction> result = transactionRepository.findByUid(nonExistingTransactionUid);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should insert transaction and return its UID")
  void shouldInsertTransactionAndReturnUid() {
    UUID newTransactionUid = UUID.randomUUID();
    Transaction transaction =
        new Transaction(
            newTransactionUid,
            customerId,
            TransactionType.PURCHASE,
            new BigDecimal("50.00"),
            "Purchase transaction",
            Instant.now(),
            TransactionStatus.PENDING,
            null);

    UUID result = transactionRepository.insertTransaction(transaction);

    assertThat(result).isEqualTo(newTransactionUid);

    Optional<Transaction> savedTransaction = transactionRepository.findByUid(newTransactionUid);
    assertThat(savedTransaction).isPresent();
    assertThat(savedTransaction.get().amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(savedTransaction.get().type()).isEqualTo(TransactionType.PURCHASE);
  }

  @Test
  @DisplayName("Should update transaction status")
  void shouldUpdateTransactionStatus() {
    boolean result =
        transactionRepository.updateTransactionStatusBy(
            existingTransactionUid, TransactionStatus.COMPLETED);

    assertThat(result).isTrue();

    Optional<Transaction> updatedTransaction =
        transactionRepository.findByUid(existingTransactionUid);
    assertThat(updatedTransaction).isPresent();
    assertThat(updatedTransaction.get().status()).isEqualTo(TransactionStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should return false when updating non-existing transaction status")
  void shouldReturnFalseWhenUpdatingNonExistingTransactionStatus() {
    boolean result =
        transactionRepository.updateTransactionStatusBy(
            nonExistingTransactionUid, TransactionStatus.COMPLETED);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should process top-up transaction and update customer balance")
  void shouldProcessTopUpAndReturnSuccessfulResponse() {
    var oldBalanceOptional =
        jdbcClient
            .sql("SELECT balance FROM customers WHERE id = :customerId")
            .param("customerId", customerId)
            .query(BigDecimal.class)
            .optional();

    assertThat(oldBalanceOptional).isPresent();

    BigDecimal topUpAmount = new BigDecimal("75.00");

    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, customerUid, topUpAmount);
    UUID idempotencyKey = UUID.randomUUID();

    TransactionResponse response =
        transactionRepository.processTransactionByCustomerId(customerId, request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isNotNull();
    assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);

    var updatedBalanceOptional =
        jdbcClient
            .sql("SELECT balance FROM customers WHERE id = :customerId")
            .param("customerId", customerId)
            .query(BigDecimal.class)
            .optional();

    BigDecimal oldBalance = oldBalanceOptional.get();
    BigDecimal expectedBalance = oldBalance.add(topUpAmount);
    assertThat(updatedBalanceOptional).isPresent().hasValue(expectedBalance);
  }

  @Test
  @DisplayName("Should process purchase transaction and update customer balance")
  void shouldProcessPurchaseAndReturnSuccessfulResponse() {
    var oldBalanceOptional =
        jdbcClient
            .sql("SELECT balance FROM customers WHERE id = :customerId")
            .param("customerId", customerId)
            .query(BigDecimal.class)
            .optional();

    assertThat(oldBalanceOptional).isPresent();
    System.out.println("OLD BALANCE: " + oldBalanceOptional.get());
    BigDecimal purchaseAmount = new BigDecimal("50.00");

    TransactionRequest request =
        new TransactionRequest(TransactionType.PURCHASE, customerUid, purchaseAmount);
    UUID idempotencyKey = UUID.randomUUID();

    TransactionResponse response =
        transactionRepository.processTransactionByCustomerId(customerId, request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isNotNull();
    assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);

    var updatedBalanceOptional =
        jdbcClient
            .sql("SELECT balance FROM customers WHERE id = :customerId")
            .param("customerId", customerId)
            .query(BigDecimal.class)
            .optional();

    BigDecimal oldBalance = oldBalanceOptional.get();
    BigDecimal expectedBalance = oldBalance.min(purchaseAmount);
    assertThat(updatedBalanceOptional).isPresent().hasValue(expectedBalance);
  }

  @Test
  @DisplayName("Should throw exception when top-up balance update fails")
  void shouldThrowExceptionWhenTopUpBalanceUpdateFails() {
    UUID nonExistentUid = UUID.randomUUID();

    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, nonExistentUid, new BigDecimal("75.00"));
    UUID idempotencyKey = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                transactionRepository.processTransactionByCustomerId(
                    customerId, request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("Could not update Transaction");
  }

  @Test
  @DisplayName("Should throw exception when purchase balance update fails")
  void shouldThrowExceptionWhenPurchaseBalanceUpdateFails() {
    UUID nonExistentUid = UUID.randomUUID();

    TransactionRequest request =
        new TransactionRequest(TransactionType.PURCHASE, nonExistentUid, new BigDecimal("50.00"));
    UUID idempotencyKey = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                transactionRepository.processTransactionByCustomerId(
                    customerId, request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("Could not update Transaction");
  }

  @Test
  @DisplayName("Should properly transition transaction status from PENDING to COMPLETED")
  void shouldTransitionTransactionStatusFromPendingToCompleted() {
    TransactionRepository repositorySpy = spy(transactionRepository);

    UUID testTransactionUid = UUID.randomUUID();
    doReturn(testTransactionUid).when(repositorySpy).insertTransaction(any(Transaction.class));
    doReturn(true).when(repositorySpy).updateTransactionStatusBy(any(), any());

    TransactionRequest request =
        new TransactionRequest(TransactionType.PURCHASE, customerUid, new BigDecimal("30.00"));
    UUID idempotencyKey = UUID.randomUUID();

    repositorySpy.processTransactionByCustomerId(customerId, request, idempotencyKey);
    verify(repositorySpy)
        .updateTransactionStatusBy(testTransactionUid, TransactionStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should process partial refund and return successful response")
  void shouldProcessPartialRefundAndReturnSuccessfulResponse() {
    UUID purchaseTransactionUid = UUID.randomUUID();
    Transaction purchaseTransaction =
        new Transaction(
            purchaseTransactionUid,
            customerId,
            TransactionType.PURCHASE,
            new BigDecimal("80.00"),
            "Purchase transaction",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);

    transactionRepository.insertTransaction(purchaseTransaction);

    var oldBalanceOptional =
        jdbcClient
            .sql("SELECT balance FROM customers WHERE id = :customerId")
            .param("customerId", customerId)
            .query(BigDecimal.class)
            .optional();

    assertThat(oldBalanceOptional).isPresent();
    BigDecimal oldBalance = oldBalanceOptional.get();

    BigDecimal refundAmount = new BigDecimal("30.00");
    TransactionRequest refundRequest =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND, customerUid, refundAmount, purchaseTransactionUid);
    UUID idempotencyKey = UUID.randomUUID();

    TransactionResponse response =
        transactionRepository.processTransactionByCustomerId(
            customerId, refundRequest, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isNotNull();
    assertThat(response.status()).isEqualTo(TransactionStatus.REFUNDED);

    var updatedBalanceOptional =
        jdbcClient
            .sql("SELECT balance FROM customers WHERE id = :customerId")
            .param("customerId", customerId)
            .query(BigDecimal.class)
            .optional();

    BigDecimal expectedNewBalance = oldBalance.add(refundAmount);
    assertThat(updatedBalanceOptional).isPresent().hasValue(expectedNewBalance);

    Optional<Transaction> savedRefundTransaction =
        transactionRepository.findByUid(response.transactionUid());
    assertThat(savedRefundTransaction).isPresent();
    assertThat(savedRefundTransaction.get().type()).isEqualTo(TransactionType.PARTIAL_REFUND);
    assertThat(savedRefundTransaction.get().amount()).isEqualByComparingTo(refundAmount);
    assertThat(savedRefundTransaction.get().referenceUid()).isEqualTo(purchaseTransactionUid);
  }

  @Test
  @DisplayName("Should calculate total refunded amount correctly")
  void shouldCalculateTotalRefundedAmountCorrectly() {
    UUID purchaseTransactionUid = UUID.randomUUID();
    Transaction purchaseTransaction =
        new Transaction(
            purchaseTransactionUid,
            customerId,
            TransactionType.PURCHASE,
            new BigDecimal("100.00"),
            "Purchase transaction",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);

    transactionRepository.insertTransaction(purchaseTransaction);

    UUID firstRefundUid = UUID.randomUUID();

    jdbcClient
        .sql(
            "INSERT INTO transactions (uid, customer_id, type, amount, description, "
                + "transaction_date, status, reference_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            firstRefundUid,
            customerId,
            TransactionType.PARTIAL_REFUND.toString(),
            new BigDecimal("30.00"),
            "First refund",
            Timestamp.from(Instant.now()),
            TransactionStatus.REFUNDED.toString(),
            purchaseTransactionUid)
        .update();

    BigDecimal totalRefunded =
        transactionRepository.getTotalRefundedAmountBy(purchaseTransactionUid);
    assertThat(totalRefunded).isEqualByComparingTo(new BigDecimal("30.00"));

    UUID secondRefundUid = UUID.randomUUID();

    jdbcClient
        .sql(
            "INSERT INTO transactions (uid, customer_id, type, amount, description, "
                + "transaction_date, status, reference_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            secondRefundUid,
            customerId,
            TransactionType.PARTIAL_REFUND.toString(),
            new BigDecimal("20.00"),
            "Second refund",
            Timestamp.from(Instant.now()),
            TransactionStatus.REFUNDED.toString(),
            purchaseTransactionUid)
        .update();

    BigDecimal updatedTotalRefunded =
        transactionRepository.getTotalRefundedAmountBy(purchaseTransactionUid);
    assertThat(updatedTotalRefunded).isEqualByComparingTo(new BigDecimal("50.00"));
  }
}
