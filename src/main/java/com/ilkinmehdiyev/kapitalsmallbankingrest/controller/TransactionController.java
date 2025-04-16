package com.ilkinmehdiyev.kapitalsmallbankingrest.controller;

import com.ilkinmehdiyev.kapitalsmallbankingrest.common.ResponseTemplate;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.service.TransactionService;
import com.ilkinmehdiyev.kapitalsmallbankingrest.utils.ResponseUtility;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/account/transactions")
public class TransactionController {
  private final TransactionService transactionService;

  @PostMapping
  public ResponseEntity<ResponseTemplate<TransactionResponse>> makeTransaction(
      @RequestHeader("x-idempotency-key") UUID idempotencyKey,
      @Valid @RequestBody TransactionRequest request) {
    log.info(
        "Customer [{}] requested {} transaction", request.customerUid(), request.transactionType());
    var response = transactionService.processTransaction(request, idempotencyKey);

    return new ResponseEntity<>(
        ResponseUtility.generateResponse(response, HttpStatus.OK.value(), ""), HttpStatus.OK);
  }
}
