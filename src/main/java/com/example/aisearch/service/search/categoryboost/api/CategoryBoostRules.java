package com.example.aisearch.service.search.categoryboost.api;

import java.util.Map;
import java.util.Optional;

/**
 * 검색어별 카테고리 부스팅 규칙 조회 계약.
 * policy 계층은 이 인터페이스만 의존하고, 실제 저장소 구현(JSON/DB 등)은 store 계층에서 담당한다.
 */
public interface CategoryBoostRules {
    /**
     * @param keyword 정규화된 검색어(trim 이후)
     * @return 카테고리 ID(String) -> 부스팅 점수(Double) 맵. 규칙이 없으면 empty.
     */
    Optional<Map<String, Double>> findByKeyword(String keyword);
}
