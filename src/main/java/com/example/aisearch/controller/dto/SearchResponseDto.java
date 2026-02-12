package com.example.aisearch.controller.dto;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchSortOption;

import java.util.List;

public record SearchResponseDto(
        String query,
        int page,
        int size,
        Integer minPrice,
        Integer maxPrice,
        List<Integer> categoryIds,
        SearchSortOption sort,
        long totalElements,
        int totalPages,
        int count,
        List<SearchHitResult> results
) {
}
