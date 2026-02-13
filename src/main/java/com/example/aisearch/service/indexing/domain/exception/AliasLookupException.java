package com.example.aisearch.service.indexing.domain.exception;

public class AliasLookupException extends RuntimeException {
    public AliasLookupException(String message) {
        super(message);
    }

    public AliasLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
