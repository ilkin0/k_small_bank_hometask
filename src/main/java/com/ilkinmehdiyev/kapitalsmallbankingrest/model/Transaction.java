package com.ilkinmehdiyev.kapitalsmallbankingrest.model;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Transaction(
        Long id,
        UUID uid,
        Long customerId,
        TransactionType type,
        BigDecimal amount,
        String description,
        Instant transactionDate,
        String referenceId,
        TransactionStatus status,
        Instant createdAt,
        String createdBy
) {

    public Transaction(UUID uid, Long customerId, TransactionType type, BigDecimal amount, String description, Instant transactionDate, TransactionStatus status, String referenceId) {
        this(null, uid, customerId, type, amount, description, transactionDate, referenceId, status, null, null);
    }
}
