package com.ilkinmehdiyev.kapitalsmallbankingrest.exception;

public class TransferRequestException extends RuntimeException {
    public TransferRequestException(String message) {
        super(message);
    }
}
