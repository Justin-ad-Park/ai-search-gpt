package com.example.aisearch.service.search.categoryboost.api;

/**
 * 카테고리 부스팅 규칙 재로딩 계약.
 * 운영 제어 포인트(수동 reload)와 저장소 구현을 분리하기 위해 별도 인터페이스로 둔다.
 */
public interface CategoryBoostRulesReloader {
    /**
     * 외부 저장소의 최신 룰로 교체를 시도한다.
     * 구현체는 실패 시 기존 캐시를 유지해 서비스 가용성을 우선해야 한다.
     */
    void reload();
}
