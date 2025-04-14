package com.ilkinmehdiyev.kapitalsmallbankingrest.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Customer(
    Long id,
    UUID uid,
    String name,
    String surname,
    LocalDate birthDate,
    String phoneNumber,
    BigDecimal balance) {}
