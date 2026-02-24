package com.example.aisearch.service.search.categoryboost.policy;

import co.elastic.clients.elasticsearch._types.SortOptions;
import com.example.aisearch.model.search.SearchSortOption;

import java.util.List;
import java.util.Map;

public record CategoryBoostingResult(
        boolean applyCategoryBoost,
        SearchSortOption effectiveSortOption,
        Map<String, Double> categoryBoostById
) {
    public List<SortOptions> sortOptions() {
        return effectiveSortOption.toSortOptions();
    }

    public static CategoryBoostingResult withoutBoost(SearchSortOption effectiveSortOption) {
        return new CategoryBoostingResult(false, effectiveSortOption, Map.of());
    }

    public static CategoryBoostingResult withBoost(Map<String, Double> categoryBoostById) {
        return new CategoryBoostingResult(true, SearchSortOption.CATEGORY_BOOSTING_DESC, categoryBoostById);
    }
}
