package com.example.aisearch.service.search.categoryboost.policy;

import co.elastic.clients.elasticsearch._types.SortOptions;
import com.example.aisearch.model.search.SearchSortOption;

import java.util.List;
import java.util.Map;

/**
 * @param applyCategoryBoost 카테고리 부스팅 점수 적용 여부
 * @param searchSortOption 최종 적용할 정렬 옵션
 * @param categoryBoostById 카테고리별 부스팅 점수 맵
 * String: 카테고리 ID
 * Double: 해당 카테고리에 가산할 부스팅 값
 */
public record CategoryBoostingResult(
        boolean applyCategoryBoost,
        SearchSortOption searchSortOption,
        Map<String, Double> categoryBoostById
) {
    public List<SortOptions> sortOptions() {
        return searchSortOption.toSortOptions();
    }

    public static CategoryBoostingResult withoutBoost(SearchSortOption effectiveSortOption) {
        return new CategoryBoostingResult(false, effectiveSortOption, Map.of());
    }

    public static CategoryBoostingResult withBoost(Map<String, Double> categoryBoostById) {
        return new CategoryBoostingResult(true, SearchSortOption.CATEGORY_BOOSTING_DESC, categoryBoostById);
    }
}
