package com.example.aisearch.model.search;

import com.example.aisearch.model.SearchHitResult;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public record SearchPageResult(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<SearchHitResult> results
) {
    public static SearchPageResult of(Pageable pageable, long totalElements, List<SearchHitResult> results) {
        PageImpl<SearchHitResult> page = new PageImpl<>(results, pageable, totalElements);
        return new SearchPageResult(
                // Spring Page 번호는 0부터 시작하기 때문에 API 응답 규약에 맞춰(1-based) +1로 변환한다.
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                results
        );
    }
}
