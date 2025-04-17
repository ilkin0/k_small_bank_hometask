package com.ilkinmehdiyev.kapitalsmallbankingrest.dto;

import jakarta.validation.constraints.NotEmpty;

public record RefreshTokenRequest(@NotEmpty String refreshToken) {}
