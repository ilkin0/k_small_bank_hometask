package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
    @NotNull TransactionType transactionType,
    @NotNull UUID customerUid,
    @Positive(message = "{topup.amount}") BigDecimal amount) {}
