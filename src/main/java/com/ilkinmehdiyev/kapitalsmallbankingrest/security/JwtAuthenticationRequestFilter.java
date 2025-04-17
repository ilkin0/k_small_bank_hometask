package com.ilkinmehdiyev.kapitalsmallbankingrest.security;

import com.ilkinmehdiyev.kapitalsmallbankingrest.common.HttpHeaders;
import com.ilkinmehdiyev.kapitalsmallbankingrest.service.CustomerServiceImpl;
import com.ilkinmehdiyev.kapitalsmallbankingrest.utils.SessionUser;
import com.ilkinmehdiyev.kapitalsmallbankingrest.utils.ThreadLocalStorage;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationRequestFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = getAuthentication(request);

    if (Objects.isNull(authentication)) {
      filterChain.doFilter(request, response);
      return;
    }

    SecurityContextHolder.getContext().setAuthentication(authentication);
    log.info("Authentication successful");

    filterChain.doFilter(request, response);
  }

  private Authentication getAuthentication(HttpServletRequest request) {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (Objects.isNull(authHeader) || !authHeader.startsWith(HttpHeaders.BEARER)) {
      return null;
    }

    final String jwtToken = authHeader.substring(HttpHeaders.BEARER.length());
    Claims claims = jwtService.parseToken(jwtToken);

    if (Objects.isNull(claims)) {
      return null;
    }

    String userName = claims.getSubject();
    var userDetails =
        (CustomerServiceImpl.CustomUserDetails) userDetailsService.loadUserByUsername(userName);
    ThreadLocalStorage.setSessionUser(
        new SessionUser(userDetails.id(), userDetails.uid(), userDetails.getUsername()));

    return new UsernamePasswordAuthenticationToken(
        userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
  }
}
