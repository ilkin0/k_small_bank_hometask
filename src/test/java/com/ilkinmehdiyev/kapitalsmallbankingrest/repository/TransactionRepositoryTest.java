package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ilkinmehdiyev.kapitalsmallbankingrest.config.TestLiquibaseConfig;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TopUpRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransactionException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.initalizer.PostgresSQLEmbeddedContainer;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = {PostgresSQLEmbeddedContainer.Initializer.class})
@Import(TestLiquibaseConfig.class)
class TransactionRepositoryTest {

  @Autowired private TransactionRepository transactionRepository;
  @Autowired private JdbcClient jdbcClient;
  @Mock private CustomerRepository customerRepository;

  private UUID existingTransactionUid;
  private UUID nonExistingTransactionUid;
  private Long customerId;
  private UUID customerUid;

  @BeforeEach
  void setUp() {
    existingTransactionUid = UUID.randomUUID();
    nonExistingTransactionUid = UUID.randomUUID();
    customerId = 1L;
    customerUid = UUID.randomUUID();

    populateDb();
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
            "");

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
  @DisplayName("Should process top-up and return successful response")
  void shouldProcessTopUpAndReturnSuccessfulResponse() {
    when(customerRepository.updateCustomerBalance(any(UUID.class), any(BigDecimal.class)))
        .thenReturn(true);

    TopUpRequest request = new TopUpRequest(customerUid, new BigDecimal("75.00"));
    UUID idempotencyKey = UUID.randomUUID();

    TransactionResponse response =
        transactionRepository.topUpByCustomerId(customerId, request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isNotNull();
    assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should throw exception when balance update fails")
  void shouldThrowExceptionWhenBalanceUpdateFails() {
    when(customerRepository.updateCustomerBalance(any(UUID.class), any(BigDecimal.class)))
        .thenReturn(false);

    TopUpRequest request = new TopUpRequest(customerUid, new BigDecimal("75.00"));
    UUID idempotencyKey = UUID.randomUUID();

    assertThatThrownBy(
            () -> transactionRepository.topUpByCustomerId(customerId, request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("Could not update Transaction");
  }

  private void populateDb() {
    Transaction testTransaction =
        new Transaction(
            existingTransactionUid,
            customerId,
            TransactionType.TOP_UP,
            new BigDecimal("100.00"),
            "Test transaction",
            Instant.now(),
            TransactionStatus.PENDING,
            "");

    jdbcClient
        .sql(
            """
                    INSERT INTO transactions
                    (uid, customer_id, type, amount, description, transaction_date, status, reference_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)
        .params(
            testTransaction.uid(),
            testTransaction.customerId(),
            testTransaction.type().toString(),
            testTransaction.amount(),
            testTransaction.description(),
            java.sql.Timestamp.from(testTransaction.transactionDate()),
            testTransaction.status().toString(),
            testTransaction.referenceId())
        .update();
  }
}
