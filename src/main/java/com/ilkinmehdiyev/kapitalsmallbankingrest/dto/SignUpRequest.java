package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName) {
}
