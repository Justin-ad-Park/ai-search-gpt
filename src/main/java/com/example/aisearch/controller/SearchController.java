package com.example.aisearch.controller;

import com.example.aisearch.controller.dto.SearchResponseDto;
import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.SearchPrice;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import com.example.aisearch.service.search.VectorSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@Validated
@RestController
public class SearchController {

    private final VectorSearchService vectorSearchService;

    public SearchController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @GetMapping("/api/search")
    public SearchResponseDto search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "5") @Min(1) @Max(20) Integer size,
            @RequestParam(value = "minPrice", required = false) @Min(0) Integer minPrice,
            @RequestParam(value = "maxPrice", required = false) @Min(0) Integer maxPrice,
            @RequestParam(value = "categoryId", required = false) List<Integer> categoryIds,
            @RequestParam(value = "sort", defaultValue = "RELEVANCE_DESC") SearchSortOption sortOption
    ) {
        SearchRequest request;
        try {
            SearchPrice searchPrice = (minPrice == null && maxPrice == null)
                    ? null
                    : new SearchPrice(minPrice, maxPrice);
            request = new SearchRequest(query, page, size, searchPrice, categoryIds, sortOption);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        SearchPageResult pageResult = vectorSearchService.searchPage(request);
        List<Integer> normalizedCategoryIds = categoryIds == null ? List.of() : categoryIds;
        return new SearchResponseDto(
                query,
                page,
                size,
                minPrice,
                maxPrice,
                normalizedCategoryIds,
                sortOption,
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.results().size(),
                pageResult.results()
        );
    }
}
