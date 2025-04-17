package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
    @NotNull TransactionType transactionType,
    @Positive(message = "{topup.amount}") BigDecimal amount,
    UUID referenceUid) {

  public TransactionRequest(TransactionType transactionType, BigDecimal amount) {
    this(transactionType, amount, null);
  }
}
