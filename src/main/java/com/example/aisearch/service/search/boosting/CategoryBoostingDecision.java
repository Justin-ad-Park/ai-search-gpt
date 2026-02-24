package com.example.aisearch.service.search.boosting;

import com.example.aisearch.model.search.SearchSortOption;

import java.util.Map;

public record CategoryBoostingDecision(
        boolean applyCategoryBoost,
        SearchSortOption effectiveSortOption,
        Map<String, Double> categoryBoostById
) {
    // 부스팅 미적용 경로(기존 정렬 또는 fallback 정렬 사용)
    public static CategoryBoostingDecision noBoost(SearchSortOption effectiveSortOption) {
        return new CategoryBoostingDecision(false, effectiveSortOption, Map.of());
    }

    // 키워드가 룰과 일치한 경우 카테고리 부스팅 적용 경로
    public static CategoryBoostingDecision apply(Map<String, Double> categoryBoostById) {
        return new CategoryBoostingDecision(true, SearchSortOption.CATEGORY_BOOSTING_DESC, categoryBoostById);
    }
}
