package com.ilkinmehdiyev.kapitalsmallbankingrest.service;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.JwtResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignInRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignInResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignUpRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignUpResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthenticationService {
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final CustomerServiceImpl customerService;
  
  public AuthenticationService(
      AuthenticationManager authenticationManager,
      @Qualifier("customerServiceImpl") UserDetailsService customerService,
      JwtService jwtService) {
    this.authenticationManager = authenticationManager;
    this.customerService = (CustomerServiceImpl) customerService;
    this.jwtService = jwtService;
  }

  public SignInResponse authenticateCustomer(SignInRequest request) {
    log.info("Starting to login request for customer: {}", request.phoneNumber());
    var authenticationToken =
        new UsernamePasswordAuthenticationToken(request.phoneNumber(), request.password());
    Authentication authentication = authenticationManager.authenticate(authenticationToken);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    //    var userDetails = customerService.loadUserByUsername(request.phoneNumber());
    var userDetails = (CustomerServiceImpl.CustomUserDetails) authentication.getPrincipal();
    var accessToken = jwtService.generateAccessToken(userDetails);
    var refreshToken = jwtService.generateRefreshToken(userDetails);

    var authenticatedUser = (CustomerServiceImpl.CustomUserDetails) authentication.getPrincipal();
    JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken);

    log.info("User Logged in: {}", authenticatedUser.getUsername());
    return new SignInResponse(jwtResponse, authenticatedUser.id(), authenticatedUser.getUsername());
  }

  public SignUpResponse registerCustomer(SignUpRequest request) {
    return null;
  }

  public JwtResponse refreshToken(String refreshToken) {
    return null;
  }
}
