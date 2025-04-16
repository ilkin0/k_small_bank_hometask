package com.ilkinmehdiyev.kapitalsmallbankingrest.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(CustomerNotFoundException.class)
  ProblemDetail handleCustomerNotFoundException(CustomerNotFoundException e) {
    return asProblemDetail(
        e.getMessage(),
        HttpStatus.NOT_FOUND,
        "Customer Not Found",
        "errors/not-found",
        "NOT_FOUND");
  }

  @ExceptionHandler(NoDataFoundException.class)
  ProblemDetail handleDataNotFoundException(NoDataFoundException e) {
    return asProblemDetail(
        e.getMessage(),
        HttpStatus.NOT_FOUND,
        "Requested Data Not Found",
        "errors/not-found",
        "NOT_FOUND");
  }

  @ExceptionHandler(TransactionException.class)
  ProblemDetail handleTransactionException(TransactionException e) {
    return asProblemDetail(
        e.getMessage(),
        HttpStatus.BAD_REQUEST,
        "Transaction Failed",
        "errors/bad-request",
        "BAD_REQUEST");
  }

  @ExceptionHandler(TransferRequestException.class)
  ProblemDetail handleTransferRequestException(TransferRequestException e) {
    return asProblemDetail(
        e.getMessage(),
        HttpStatus.BAD_REQUEST,
        "Transaction Request Body is Invalid",
        "errors/invalid-request-body",
        "INVALID_REQUEST_BODY");
  }

  @Override
  public ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).toList();

    ProblemDetail problemDetail =
        asProblemDetail(
            errors,
            HttpStatus.BAD_REQUEST,
            "Validasiya xetalari movcuddur",
            "errors/validations",
            "Validation",
            null);

    return new ResponseEntity<>(problemDetail, headers, status);
  }

  private ProblemDetail asProblemDetail(
      String message, HttpStatus httpStatus, String title, String uri, String errorCategory) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, message);
    problemDetail.setTitle(title);
    problemDetail.setType(URI.create(uri));
    problemDetail.setProperty("errorCategory", errorCategory);
    problemDetail.setProperty("timestamp", Instant.now());
    return problemDetail;
  }

  public static ProblemDetail asProblemDetail(
      List<String> messages,
      HttpStatus httpStatus,
      String title,
      String uri,
      String errorCategory,
      Integer code) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatus);
    problemDetail.setTitle(title);
    problemDetail.setType(URI.create(uri));
    problemDetail.setProperty("errorCategory", errorCategory);
    problemDetail.setProperty("timestamp", Instant.now());
    problemDetail.setProperty("details", messages);

    if (code != null) {
      problemDetail.setProperty("code", code);
    }
    return problemDetail;
  }
}
