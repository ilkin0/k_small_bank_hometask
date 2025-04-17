package com.ilkinmehdiyev.kapitalsmallbankingrest.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
  private final JwtProperties jwtProperties;

  public record JwtProperties(
      String secretKey,
      long tokenValidityInSeconds,
      long tokenValidityInSecondsForRememberMe,
      RefreshToken refreshToken) {

    public record RefreshToken(long expiration) {}
  }
}
