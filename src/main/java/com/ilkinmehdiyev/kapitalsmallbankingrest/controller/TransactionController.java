package com.ilkinmehdiyev.kapitalsmallbankingrest.controller;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.service.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
  private final TransactionService transactionService;

  @PostMapping
  public ResponseEntity<TransactionResponse> topUpAccount(
      @RequestHeader("x-idempotency-key") UUID idempotencyKey,
      @Valid @RequestBody TransactionRequest request) {
    log.info("Top up account request received");
    var response = transactionService.processTransaction(request, idempotencyKey);

    return ResponseEntity.ok(response);
  }
}
