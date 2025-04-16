package com.ilkinmehdiyev.kapitalsmallbankingrest.utils;

import com.ilkinmehdiyev.kapitalsmallbankingrest.common.ResponseTemplate;
import com.ilkinmehdiyev.kapitalsmallbankingrest.common.StatusResponse;

public class ResponseUtility {

  public static <T> ResponseTemplate<T> generateResponse(
      T responseData, Integer statusCode, String message) {
    return new ResponseTemplate<>(responseData, new StatusResponse(statusCode, message));
  }

  public static <T> ResponseTemplate<T> generateResponse(Integer statusCode, String message) {
    return new ResponseTemplate<>(null, new StatusResponse(statusCode, message));
  }
}
