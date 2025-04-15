package com.ilkinmehdiyev.kapitalsmallbankingrest.model;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Transaction(
    Long id,
    UUID uid,
    Long customerId,
    TransactionType type,
    BigDecimal amount,
    String description,
    Instant transactionDate,
    UUID referenceUid,
    TransactionStatus status,
    Instant createdAt,
    String createdBy) {

  public Transaction(
      UUID uid,
      Long customerId,
      TransactionType type,
      BigDecimal amount,
      String description,
      Instant transactionDate,
      TransactionStatus status,
      UUID referenceUid) {
    this(
        null,
        uid,
        customerId,
        type,
        amount,
        description,
        transactionDate,
        referenceUid,
        status,
        null,
        null);
  }
}
