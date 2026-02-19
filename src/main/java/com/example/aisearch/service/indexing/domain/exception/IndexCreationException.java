package com.example.aisearch.service.indexing.domain.exception;

/** 인덱스 생성 단계 실패를 나타내는 예외 */
public class IndexCreationException extends RuntimeException {
    public IndexCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
