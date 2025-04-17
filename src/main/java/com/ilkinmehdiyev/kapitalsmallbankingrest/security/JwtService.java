package com.ilkinmehdiyev.kapitalsmallbankingrest.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

  private final SecurityProperties securityProperties;

  public Claims parseToken(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSignInKey(securityProperties.getJwtProperties().secretKey()))
          .clockSkewSeconds(5)
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      log.error("JWT token expired", e);
      throw new JwtException("JWT token expired");
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public String extractEmail(String jwtToken) {
    return extractClaim(jwtToken, Claims::getSubject);
  }

  public <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {
    final Claims claims = parseToken(jwtToken);
    return claimsResolver.apply(claims);
  }

  public boolean validateToken(String jwtToken, String username) {
    final String email = extractEmail(jwtToken);
    return email.equals(username) && !isTokenExpired(jwtToken);
  }

  private boolean isTokenExpired(String jwtToken) {
    return extractExpiration(jwtToken).before(new Date());
  }

  private Date extractExpiration(String jwtToken) {
    return extractClaim(jwtToken, Claims::getExpiration);
  }

  public <U extends UserDetails> String generateAccessToken(U user) {
    return generateToken(user, securityProperties.getJwtProperties().tokenValidityInSeconds());
  }

  public <U extends UserDetails> String generateRefreshToken(U user) {
    return generateToken(
        user, securityProperties.getJwtProperties().tokenValidityInSecondsForRememberMe());
  }

  public <U extends UserDetails> String generateToken(U user, long expirationMs) {

    Duration duration = Duration.of(expirationMs, ChronoUnit.MILLIS);
    JwtBuilder jwtBuilder =
        Jwts.builder()
            .subject(user.getUsername())
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(duration)))
            .header()
            .add(Map.of("type", "JWT"))
            .and()
            .signWith(getSignInKey(securityProperties.getJwtProperties().secretKey()));

    addClaims(user, jwtBuilder);
    return jwtBuilder.compact();
  }

  private <U extends UserDetails> void addClaims(U user, JwtBuilder jwtBuilder) {
    Map<String, String> claimsMap = new HashMap<>();

    addAuthoritiesClaims(claimsMap, user.getAuthorities());
    claimsMap.put("username", user.getUsername());

    jwtBuilder.claims().add(claimsMap);
  }

  private void addAuthoritiesClaims(
      Map<String, String> claimsMap, Collection<? extends GrantedAuthority> userAuthorities) {
    userAuthorities.stream()
        .map(GrantedAuthority::getAuthority)
        .forEach(auth -> claimsMap.put("authority", auth));
  }

  private Duration getDuration(boolean remember) {
    return remember ? Duration.of(1, ChronoUnit.DAYS) : Duration.of(3, ChronoUnit.HOURS);
  }

  private SecretKey getSignInKey(String secret) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
