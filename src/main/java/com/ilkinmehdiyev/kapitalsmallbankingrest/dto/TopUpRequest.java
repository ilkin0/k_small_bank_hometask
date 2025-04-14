package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TopUpRequest(
        @NotNull UUID customerUid,
        @Positive(message = "{topup.amount}") BigDecimal amount
) {
}
