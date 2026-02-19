package com.example.aisearch.service.indexing.domain.exception;

/** read alias 전환 단계 실패를 나타내는 예외 */
public class AliasSwapException extends RuntimeException {
    public AliasSwapException(String message, Throwable cause) {
        super(message, cause);
    }
}
