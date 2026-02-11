package com.example.aisearch.controller;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.service.search.VectorSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
public class SearchController {

    private final VectorSearchService vectorSearchService;

    public SearchController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @GetMapping("/api/search")
    public Map<String, Object> search(
            @RequestParam("q") @NotBlank String query,
            @RequestParam(value = "size", defaultValue = "5") @Min(1) @Max(20) int size
    ) {
        // 요청 파라미터 검증 후 벡터 검색 수행
        List<SearchHitResult> results = vectorSearchService.search(query, size);
        // 간단한 JSON 응답 구성
        return Map.of(
                "query", query,
                "size", size,
                "count", results.size(),
                "results", results
        );
    }
}
