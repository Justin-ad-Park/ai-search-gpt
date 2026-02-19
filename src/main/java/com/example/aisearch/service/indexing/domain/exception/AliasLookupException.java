package com.example.aisearch.service.indexing.domain.exception;

/** read alias 조회 단계 실패를 나타내는 예외 */
public class AliasLookupException extends RuntimeException {
    public AliasLookupException(String message) {
        super(message);
    }

    public AliasLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
