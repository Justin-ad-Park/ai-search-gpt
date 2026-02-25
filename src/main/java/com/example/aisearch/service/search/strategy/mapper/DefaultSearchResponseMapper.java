package com.example.aisearch.service.search.strategy.mapper;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchPageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 검색 응답을 애플리케이션 응답 모델로 변환한다.
 */
@Component
public class DefaultSearchResponseMapper {

    public SearchPageResult toPageResult(SearchResponse<Map> response, Pageable pageable) {
        List<SearchHitResult> results = toResults(response);
        return SearchPageResult.of(pageable, extractTotalHits(response), results);
    }

    private List<SearchHitResult> toResults(SearchResponse<Map> response) {
        return response.hits().hits().stream()
                .map(hit -> new SearchHitResult(hit.id(), hit.score(), stripVector(hit.source())))
                .toList();
    }

    private Map<String, Object> stripVector(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        // 응답 payload 축소를 위해 대용량 벡터 필드를 제거한다.
        Map<String, Object> filtered = new HashMap<>(source);
        filtered.remove("product_vector");
        return filtered;
    }

    private long extractTotalHits(SearchResponse<Map> response) {
        if (response.hits() == null || response.hits().total() == null) {
            return 0L;
        }
        return response.hits().total().value();
    }
}

