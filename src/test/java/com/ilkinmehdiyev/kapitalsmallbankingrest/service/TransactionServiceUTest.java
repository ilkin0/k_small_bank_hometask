package com.ilkinmehdiyev.kapitalsmallbankingrest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.CustomerNotFoundException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.NoDataFoundException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransactionException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransferRequestException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Customer;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.CustomerRepository;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceUTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private TransactionRepository transactionRepository;
  @InjectMocks private TransactionService transactionService;

  private UUID customerUid;
  private UUID transactionUid;
  private Customer testCustomer;
  private Transaction testTransaction;

  @BeforeEach
  void setUp() {
    customerUid = UUID.randomUUID();
    transactionUid = UUID.randomUUID();

    testCustomer =
        Customer.builder()
            .id(1L)
            .uid(customerUid)
            .name("Eldar")
            .surname("Mammadov")
            .birthDate(LocalDate.now())
            .phoneNumber("+994501234567")
            .balance(new BigDecimal("100.00"))
            .build();

    testTransaction =
        new Transaction(
            transactionUid,
            1L,
            TransactionType.TOP_UP,
            new BigDecimal("50.00"),
            "Test transaction",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);
  }

  @Test
  @DisplayName("Should successfully process a top-up transaction")
  void shouldProcessTopUpTransaction() {
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, customerUid, new BigDecimal("50.00"));
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.processTransactionByCustomerId(
            testCustomer.id(), request, idempotencyKey))
        .thenReturn(
            new TransactionResponse(transactionUid, TransactionStatus.COMPLETED, Instant.now()));

    TransactionResponse response = transactionService.processTransaction(request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isEqualTo(transactionUid);
    assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);

    verify(customerRepository).getCustomerByUidForUpdate(customerUid);
    verify(transactionRepository).findByUid(idempotencyKey);
    verify(transactionRepository)
        .processTransactionByCustomerId(testCustomer.id(), request, idempotencyKey);
  }

  @Test
  @DisplayName("Should successfully process a purchase transaction with sufficient balance")
  void shouldProcessPurchaseTransactionWithSufficientBalance() {
    TransactionRequest request =
        new TransactionRequest(TransactionType.PURCHASE, customerUid, new BigDecimal("50.00"));
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.processTransactionByCustomerId(
            testCustomer.id(), request, idempotencyKey))
        .thenReturn(
            new TransactionResponse(transactionUid, TransactionStatus.COMPLETED, Instant.now()));

    TransactionResponse response = transactionService.processTransaction(request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isEqualTo(transactionUid);
    assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should throw exception when processing purchase with insufficient balance")
  void shouldThrowExceptionWhenProcessingPurchaseWithInsufficientBalance() {
    Customer lowBalanceCustomer =
        Customer.builder()
            .id(1L)
            .uid(customerUid)
            .name("Eldar")
            .surname("Mammadov")
            .birthDate(LocalDate.now())
            .phoneNumber("+994501234567")
            .balance(new BigDecimal("25.00"))
            .build();

    TransactionRequest request =
        new TransactionRequest(TransactionType.PURCHASE, customerUid, new BigDecimal("50.00"));
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(lowBalanceCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(TransferRequestException.class)
        .hasMessageContaining("Customer does not have sufficient balance");

    verify(transactionRepository, never()).processTransactionByCustomerId(any(), any(), any());
  }

  @Test
  @DisplayName("Should return existing transaction when processing with same idempotency key")
  void shouldReturnExistingTransactionWhenProcessingWithSameIdempotencyKey() {
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, customerUid, new BigDecimal("50.00"));
    UUID idempotencyKey = transactionUid;

    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.of(testTransaction));

    TransactionResponse response = transactionService.processTransaction(request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.transactionUid()).isEqualTo(transactionUid);
    assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);

    verify(customerRepository, never()).getCustomerByUidForUpdate(any());
    verify(transactionRepository, never()).processTransactionByCustomerId(any(), any(), any());
  }

  @Test
  @DisplayName("Should throw exception when customer is not found")
  void shouldThrowExceptionWhenCustomerIsNotFound() {
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, customerUid, new BigDecimal("50.00"));
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(CustomerNotFoundException.class)
        .hasMessageContaining("Customer with id");
  }

  @Test
  @DisplayName("Should throw exception when customer uid is null")
  void shouldThrowExceptionWhenCustomerUidIsNull() {
    TransactionRequest request =
        new TransactionRequest(TransactionType.TOP_UP, null, new BigDecimal("50.00"));
    UUID idempotencyKey = UUID.randomUUID();

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(NoDataFoundException.class)
        .hasMessageContaining("Customer ID is required");

    verify(transactionRepository, never()).findByUid(any());
    verify(customerRepository, never()).getCustomerByUidForUpdate(any());
  }

  @Test
  @DisplayName("Should throw exception when amount is negative or zero")
  void shouldThrowExceptionWhenAmountIsNegativeOrZero() {
    TransactionRequest zeroRequest =
        new TransactionRequest(TransactionType.TOP_UP, customerUid, BigDecimal.ZERO);
    UUID idempotencyKey = UUID.randomUUID();

    assertThatThrownBy(() -> transactionService.processTransaction(zeroRequest, idempotencyKey))
        .isInstanceOf(TransferRequestException.class)
        .hasMessageContaining("Amount must be greater than zero");

    TransactionRequest negativeRequest =
        new TransactionRequest(TransactionType.TOP_UP, customerUid, new BigDecimal("-10.00"));

    assertThatThrownBy(() -> transactionService.processTransaction(negativeRequest, idempotencyKey))
        .isInstanceOf(TransferRequestException.class)
        .hasMessageContaining("Amount must be greater than zero");
  }

  @Test
  @DisplayName("Should successfully process a partial refund transaction")
  void shouldProcessPartialRefundTransaction() {
    UUID referenceTransactionUid = UUID.randomUUID();
    Transaction referenceTransaction =
        new Transaction(
            referenceTransactionUid,
            1L,
            TransactionType.PURCHASE,
            new BigDecimal("100.00"),
            "Original purchase",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);

    BigDecimal refundAmount = new BigDecimal("30.00");
    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND, customerUid, refundAmount, referenceTransactionUid);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(referenceTransactionUid))
        .thenReturn(Optional.of(referenceTransaction));
    when(transactionRepository.getTotalRefundedAmountBy(referenceTransactionUid))
        .thenReturn(BigDecimal.ZERO);
    when(transactionRepository.processTransactionByCustomerId(
            testCustomer.id(), request, idempotencyKey))
        .thenReturn(
            new TransactionResponse(UUID.randomUUID(), TransactionStatus.REFUNDED, Instant.now()));

    TransactionResponse response = transactionService.processTransaction(request, idempotencyKey);

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(TransactionStatus.REFUNDED);

    verify(transactionRepository).findByUid(referenceTransactionUid);
    verify(transactionRepository).getTotalRefundedAmountBy(referenceTransactionUid);
    verify(transactionRepository)
        .processTransactionByCustomerId(testCustomer.id(), request, idempotencyKey);
  }

  @Test
  @DisplayName("Should throw exception when refund amount exceeds original transaction amount")
  void shouldThrowExceptionWhenRefundAmountExceedsOriginalAmount() {
    UUID referenceTransactionUid = UUID.randomUUID();
    Transaction referenceTransaction =
        new Transaction(
            referenceTransactionUid,
            1L,
            TransactionType.PURCHASE,
            new BigDecimal("50.00"),
            "Original purchase",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);

    BigDecimal refundAmount = new BigDecimal("75.00");
    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND, customerUid, refundAmount, referenceTransactionUid);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(referenceTransactionUid))
        .thenReturn(Optional.of(referenceTransaction));
    when(transactionRepository.getTotalRefundedAmountBy(referenceTransactionUid))
        .thenReturn(BigDecimal.ZERO);

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("exceed original transaction amount");
  }

  @Test
  @DisplayName("Should throw exception when refunding a non-purchase transaction")
  void shouldThrowExceptionWhenRefundingNonPurchaseTransaction() {
    UUID referenceTransactionUid = UUID.randomUUID();
    Transaction referenceTransaction =
        new Transaction(
            referenceTransactionUid,
            1L,
            TransactionType.TOP_UP,
            new BigDecimal("100.00"),
            "Original top-up",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);

    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND,
            customerUid,
            new BigDecimal("30.00"),
            referenceTransactionUid);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(referenceTransactionUid))
        .thenReturn(Optional.of(referenceTransaction));
    when(transactionRepository.getTotalRefundedAmountBy(referenceTransactionUid))
        .thenReturn(BigDecimal.ZERO);

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("Only purchase transactions can be refunded");
  }

  @Test
  @DisplayName("Should throw exception when reference transaction is not in COMPLETED status")
  void shouldThrowExceptionWhenReferenceTransactionNotCompleted() {
    UUID referenceTransactionUid = UUID.randomUUID();
    Transaction referenceTransaction =
        new Transaction(
            referenceTransactionUid,
            1L,
            TransactionType.PURCHASE,
            new BigDecimal("100.00"),
            "Pending purchase",
            Instant.now(),
            TransactionStatus.PENDING,
            null);

    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND,
            customerUid,
            new BigDecimal("30.00"),
            referenceTransactionUid);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(referenceTransactionUid))
        .thenReturn(Optional.of(referenceTransaction));
    when(transactionRepository.getTotalRefundedAmountBy(referenceTransactionUid))
        .thenReturn(BigDecimal.ZERO);

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("not COMPLETED");
  }

  @Test
  @DisplayName("Should throw exception when total refunds would exceed original amount")
  void shouldThrowExceptionWhenTotalRefundsExceedOriginalAmount() {
    UUID referenceTransactionUid = UUID.randomUUID();
    Transaction referenceTransaction =
        new Transaction(
            referenceTransactionUid,
            1L,
            TransactionType.PURCHASE,
            new BigDecimal("100.00"),
            "Original purchase",
            Instant.now(),
            TransactionStatus.COMPLETED,
            null);

    BigDecimal previousRefunds = new BigDecimal("80.00");
    BigDecimal refundAmount = new BigDecimal("30.00");

    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND, customerUid, refundAmount, referenceTransactionUid);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(referenceTransactionUid))
        .thenReturn(Optional.of(referenceTransaction));
    when(transactionRepository.getTotalRefundedAmountBy(referenceTransactionUid))
        .thenReturn(previousRefunds);

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(TransactionException.class)
        .hasMessageContaining("Total refunded amount");
  }

  @Test
  @DisplayName("Should throw exception when refund reference transaction is not found")
  void shouldThrowExceptionWhenRefundReferenceTransactionNotFound() {
    UUID nonExistentReferenceUid = UUID.randomUUID();

    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND,
            customerUid,
            new BigDecimal("30.00"),
            nonExistentReferenceUid);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());
    when(transactionRepository.findByUid(nonExistentReferenceUid)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(NoDataFoundException.class)
        .hasMessageContaining("Refund transaction with Reference uid");
  }

  @Test
  @DisplayName("Should throw exception when refund reference UID is null")
  void shouldThrowExceptionWhenRefundReferenceUidIsNull() {
    TransactionRequest request =
        new TransactionRequest(
            TransactionType.PARTIAL_REFUND, customerUid, new BigDecimal("30.00"), null);
    UUID idempotencyKey = UUID.randomUUID();

    when(customerRepository.getCustomerByUidForUpdate(customerUid))
        .thenReturn(Optional.of(testCustomer));
    when(transactionRepository.findByUid(idempotencyKey)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> transactionService.processTransaction(request, idempotencyKey))
        .isInstanceOf(NoDataFoundException.class)
        .hasMessageContaining("Reference transaction ID is required");
  }
}
