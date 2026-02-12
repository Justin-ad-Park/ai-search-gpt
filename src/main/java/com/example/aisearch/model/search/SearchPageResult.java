package com.example.aisearch.model.search;

import com.example.aisearch.model.SearchHitResult;

import java.util.List;

public record SearchPageResult(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<SearchHitResult> results
) {
    public static SearchPageResult of(SearchRequest request, long totalElements, List<SearchHitResult> results) {
        int totalPages = (int) Math.ceil((double) totalElements / request.size());
        return new SearchPageResult(request.page(), request.size(), totalElements, totalPages, results);
    }
}
