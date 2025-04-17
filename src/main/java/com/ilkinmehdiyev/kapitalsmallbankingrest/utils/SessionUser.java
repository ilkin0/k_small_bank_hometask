package com.ilkinmehdiyev.kapitalsmallbankingrest.utils;

import java.util.UUID;

public record SessionUser(Long userId, UUID uid, String phoneNumber) {}
