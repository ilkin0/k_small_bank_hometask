package com.ilkinmehdiyev.kapitalsmallbankingrest.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record Token(
    Long id,
    String value,
    Instant issuedAt,
    Instant expiresAt,
    boolean isExpired,
    boolean isActive,
    Long customerId) {

  public boolean isValid() {
    return isActive && expiresAt.isAfter(Instant.now());
  }
}
