package com.ilkinmehdiyev.kapitalsmallbankingrest.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResponseTemplate<T> {
    private T data;
    private StatusResponse status;
}