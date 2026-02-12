package com.example.aisearch.model.search;

import java.util.List;

public record SearchRequest(
        String query,
        Integer page,
        Integer size,
        SearchPrice searchPrice,
        List<Integer> categoryIds,
        SearchSortOption sortOption
) {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 5;
    public static final int MAX_SIZE = 20;

    public SearchRequest {
        query = (query == null || query.isBlank()) ? null : query.trim();
        page = (page == null) ? DEFAULT_PAGE : page;
        size = (size == null) ? DEFAULT_SIZE : size;

        if (page < 1) {
            throw new IllegalArgumentException("page는 1 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size는 1~20 범위여야 합니다.");
        }

        if (categoryIds != null) {
            categoryIds = categoryIds.stream().filter(id -> id != null).distinct().toList();
        }

        sortOption = (sortOption == null) ? SearchSortOption.RELEVANCE_DESC : sortOption;
    }

    public SearchRequest(String query, Integer size, SearchPrice searchPrice, List<Integer> categoryIds) {
        this(query, null, size, searchPrice, categoryIds, null);
    }

    public SearchRequest(String query, Integer size, SearchPrice searchPrice, List<Integer> categoryIds, SearchSortOption sortOption) {
        this(query, null, size, searchPrice, categoryIds, sortOption);
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

    public int from() {
        return (page - 1) * size;
    }
}
