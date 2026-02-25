package com.example.aisearch.service.search.categoryboost.policy;

import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import com.example.aisearch.service.search.categoryboost.api.CategoryBoostRules;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 검색 요청의 정렬/검색어 상태와 룰 매칭 결과를 바탕으로
 * "카테고리 부스팅 적용 여부 + 최종 정렬 옵션"을 결정한다.
 */
@Component
public class CategoryBoostingDecider {

    private final CategoryBoostRules categoryBoostRules;

    public CategoryBoostingDecider(CategoryBoostRules categoryBoostRules) {
        this.categoryBoostRules = categoryBoostRules;
    }

    /**
     * CATEGORY_BOOSTING_DESC 요청에서만 룰 매칭을 시도하고,
     * query 없음/불일치 시 RELEVANCE_DESC 로 안전하게 fallback 한다.
     */
    public CategoryBoostingResult decide(SearchRequest request) {
        // CATEGORY_BOOSTING_DESC 요청이 아니면 부스팅 없이 요청 정렬을 그대로 사용한다.
        if (request.sortOption() != SearchSortOption.CATEGORY_BOOSTING_DESC) {
            return CategoryBoostingResult.withoutBoost(request.sortOption());
        }

        String query = request.query();
        // 카테고리 부스팅은 검색어 기반 규칙이므로 query 가 없으면 적용할 수 없다.
        // 이 경우 일반 연관도 정렬(RELEVANCE_DESC)로 동작을 고정한다.
        if (query == null || query.isBlank()) {
            return CategoryBoostingResult.withoutBoost(SearchSortOption.RELEVANCE_DESC);
        }

        Optional<Map<String, Double>> boostByKeyword = categoryBoostRules.findByKeyword(query);
        // 검색어에 매칭되는 부스팅 규칙이 없으면 부스팅 없이 연관도 정렬을 사용한다.
        if (boostByKeyword.isEmpty()) {
            return CategoryBoostingResult.withoutBoost(SearchSortOption.RELEVANCE_DESC);
        }

        // 규칙이 존재할 때만 카테고리 부스팅 점수를 적용한다.
        return CategoryBoostingResult.withBoost(boostByKeyword.get());
    }
}
