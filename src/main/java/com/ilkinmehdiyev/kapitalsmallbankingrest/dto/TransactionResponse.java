package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionUid,
        TransactionStatus status,
        Instant transactionDate
) {
}
