package com.ilkinmehdiyev.kapitalsmallbankingrest.common;

public final class HttpHeaders {
  private HttpHeaders() {}

  public static final String BEARER = "Bearer ";
  public static final String AUTHORIZATION = "Authorization";
  public static final String X_IDEMPOTENCY_KEY = "x-idempotency-key";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String REQUESTOR_TYPE = "Requestor-Type";
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
}
