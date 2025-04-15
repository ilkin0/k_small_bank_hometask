package com.ilkinmehdiyev.kapitalsmallbankingrest.service;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.CustomerNotFoundException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.NoDataFoundException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransferRequestException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.CustomerRepository;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
  private final CustomerRepository customerRepository;
  private final TransactionRepository transactionRepository;

  @Transactional
  public TransactionResponse processTransaction(TransactionRequest request, UUID idempotencyKey) {
    UUID customerUid = request.customerUid();
    BigDecimal amount = request.amount();

    log.info("Top up customer with uid: {}", customerUid);
    validateTransactionRequest(customerUid, amount);

    Optional<TransactionResponse> transactionResponseOptional =
        getTransactionResponseByUid(idempotencyKey);

    if (transactionResponseOptional.isPresent()) {
      return transactionResponseOptional.get();
    }

    var customer =
        customerRepository
            .getCustomerByUidForUpdate(customerUid)
            .orElseThrow(
                () -> {
                  log.error("Customer with id {} not found", customerUid);
                  return new CustomerNotFoundException(
                      "Customer with id: [%s] not found".formatted(customerUid));
                });
    validateCustomerBalance(customer.balance(), request);

    return transactionRepository.processTransactionByCustomerId(
        customer.id(), request, idempotencyKey);
  }

  private void validateCustomerBalance(BigDecimal currentBalance, TransactionRequest request) {
    if (!TransactionType.TOP_UP.equals(request.transactionType())
        && currentBalance.compareTo(request.amount()) < 0) {
      log.warn(
          "Customer does not have sufficient balance, current balance: [{}], request amount: [{}]",
          currentBalance,
          request.amount());

      throw new TransferRequestException(
          "Customer does not have sufficient balance, current balance: [%f], request amount: [%f]"
              .formatted(currentBalance, request.amount()));
    }
  }

  private Optional<TransactionResponse> getTransactionResponseByUid(UUID idempotencyKey) {
    var transactionOptional = transactionRepository.findByUid(idempotencyKey);
    if (transactionOptional.isPresent()) {
      log.warn("Top up request with x-idempotency [{}] already exists", idempotencyKey);
      return Optional.of(mapToTransactionResponse(transactionOptional.get()));
    }
    return Optional.empty();
  }

  private TransactionResponse mapToTransactionResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.uid(), transaction.status(), transaction.transactionDate());
  }

  private void validateTransactionRequest(UUID customerUid, BigDecimal amount) {
    if (Objects.isNull(customerUid)) {
      throw new NoDataFoundException("Customer ID is required");
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.error("Amount must be greater than zero");
      throw new TransferRequestException(
          "Amount must be greater than zero, amount: %f".formatted(amount));
    }
  }
}
