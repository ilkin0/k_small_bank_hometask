package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(@NotBlank String phoneNumber, @NotBlank String password) {}
