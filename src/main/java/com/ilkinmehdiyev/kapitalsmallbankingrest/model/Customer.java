package com.ilkinmehdiyev.kapitalsmallbankingrest.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record Customer(
        Long id,
        UUID uid,
        String name,
        String surname,
        LocalDate dateOfBirth,
        String phoneNumber,
        BigDecimal balance
) {
}
