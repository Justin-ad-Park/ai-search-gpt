package com.example.aisearch.model.search;

import java.util.List;

public record ProductSearchRequest(
        String query,
        SearchPrice searchPrice,
        List<Integer> categoryIds,
        SearchSortOption sortOption
) {
    public ProductSearchRequest {
        query = (query == null || query.isBlank()) ? null : query.trim();

        if (categoryIds != null) {
            categoryIds = categoryIds.stream().filter(id -> id != null).distinct().toList();
        }

        if (sortOption == null) {
            sortOption = SearchSortOption.RELEVANCE_DESC;
        }

        // CATEGORY_BOOSTING_DESC 는 검색어(query)가 있을 때만 의미가 있다.
        // query 가 없으면 카테고리 부스팅을 계산할 수 없으므로 RELEVANCE_DESC 로 정규화한다.
        if (query == null && sortOption == SearchSortOption.CATEGORY_BOOSTING_DESC) {
            sortOption = SearchSortOption.RELEVANCE_DESC;
        }
    }

    public boolean hasQuery() {
        return query != null;
    }

    public boolean hasPriceCondition() {
        return searchPrice != null && !searchPrice.isEmpty();
    }

    public boolean hasCategoryCondition() {
        return categoryIds != null && !categoryIds.isEmpty();
    }
}
