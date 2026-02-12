package com.example.aisearch.model.search;

import java.util.List;

public record SearchRequest(
        String query,
        Integer size,
        SearchPrice searchPrice,
        List<Integer> categoryIds
) {
    public static final int DEFAULT_SIZE = 5;
    public static final int MAX_SIZE = 20;

    public SearchRequest {
        query = (query == null || query.isBlank()) ? null : query.trim();
        size = (size == null) ? DEFAULT_SIZE : size;

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size는 1~20 범위여야 합니다.");
        }

        if (categoryIds != null) {
            categoryIds = categoryIds.stream().filter(id -> id != null).distinct().toList();
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
