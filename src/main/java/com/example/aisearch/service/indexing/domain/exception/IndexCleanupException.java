package com.example.aisearch.service.indexing.domain.exception;

public class IndexCleanupException extends RuntimeException {
    public IndexCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
