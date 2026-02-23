package com.example.aisearch.service.search.boosting;

import com.example.aisearch.model.search.SearchSortOption;

import java.util.Map;

public record CategoryBoostingDecision(
        boolean applyCategoryBoost,
        SearchSortOption effectiveSortOption,
        Map<String, Double> categoryBoostById
) {
    public static CategoryBoostingDecision noBoost(SearchSortOption effectiveSortOption) {
        return new CategoryBoostingDecision(false, effectiveSortOption, Map.of());
    }

    public static CategoryBoostingDecision apply(Map<String, Double> categoryBoostById) {
        return new CategoryBoostingDecision(true, SearchSortOption.CATEGORY_BOOSTING_DESC, categoryBoostById);
    }
}
