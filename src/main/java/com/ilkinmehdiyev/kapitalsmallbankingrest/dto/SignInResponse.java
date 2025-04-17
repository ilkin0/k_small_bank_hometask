package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

public record SignInResponse(JwtResponse token, Long userId, String fullName) {}
