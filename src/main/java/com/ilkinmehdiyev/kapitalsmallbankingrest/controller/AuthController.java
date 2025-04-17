package com.ilkinmehdiyev.kapitalsmallbankingrest.controller;

import com.ilkinmehdiyev.kapitalsmallbankingrest.common.ResponseTemplate;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.JwtResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.RefreshTokenRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignInRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignInResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignUpRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.SignUpResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.service.AuthenticationService;
import com.ilkinmehdiyev.kapitalsmallbankingrest.utils.ResponseUtility;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationService authService;

  @PostMapping("/sign-in")
  public ResponseEntity<ResponseTemplate<SignInResponse>> login(
      @RequestBody @Valid SignInRequest signInRequest, HttpServletResponse response) {
    log.info("Received request to sign in");
    SignInResponse signInResponse = authService.authenticateCustomer(signInRequest);

    Cookie refreshTokenCookie = new Cookie("refreshToken", signInResponse.token().refreshToken());
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setPath("/auth/refresh-token");
    response.addCookie(refreshTokenCookie);

    return new ResponseEntity<>(
        ResponseUtility.generateResponse(signInResponse, HttpStatus.OK.value(), ""), HttpStatus.OK);
  }

  @PostMapping("/sign-up")
  public ResponseEntity<ResponseTemplate<SignUpResponse>> signUp(
      @RequestBody @Valid SignUpRequest request) {
    log.info("Received request to sign up");
    SignUpResponse signUpResponse = authService.registerCustomer(request);

    return new ResponseEntity<>(
        ResponseUtility.generateResponse(
            signUpResponse, HttpStatus.CREATED.value(), "USER CREATED"),
        HttpStatus.CREATED);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ResponseTemplate<JwtResponse>> refreshToken(
      @RequestBody @Valid RefreshTokenRequest request) {
    log.info("Received request to refresh token");

    JwtResponse response = authService.refreshToken(request.refreshToken());
    return new ResponseEntity<>(
        ResponseUtility.generateResponse(response, HttpStatus.OK.value(), "OK"), HttpStatus.OK);
  }
}
