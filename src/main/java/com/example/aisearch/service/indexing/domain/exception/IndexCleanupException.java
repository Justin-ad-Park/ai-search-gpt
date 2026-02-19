package com.example.aisearch.service.indexing.domain.exception;

/** 인덱스 정리(삭제) 단계 실패를 나타내는 예외 */
public class IndexCleanupException extends RuntimeException {
    public IndexCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
