package com.example.aisearch.model.search;

import java.util.List;

public record SearchRequest(
        String query,
        SearchPrice searchPrice,
        List<Integer> categoryIds,
        SearchSortOption sortOption
) {
    public SearchRequest {
        query = (query == null || query.isBlank()) ? null : query.trim();

        if (categoryIds != null) {
            categoryIds = categoryIds.stream().filter(id -> id != null).distinct().toList();
        }

        sortOption = (sortOption == null) ? SearchSortOption.RELEVANCE_DESC : sortOption;
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
