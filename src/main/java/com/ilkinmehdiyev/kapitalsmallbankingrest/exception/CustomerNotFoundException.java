package com.ilkinmehdiyev.kapitalsmallbankingrest.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
