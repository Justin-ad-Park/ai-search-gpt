package com.example.aisearch.service.search;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.SearchPagingPolicy;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.service.search.strategy.SearchStrategy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

    private final SearchStrategy searchStrategy;

    public VectorSearchService(
            SearchStrategy searchStrategy
    ) {
        this.searchStrategy = searchStrategy;
    }

    public SearchPageResult searchPage(SearchRequest searchRequest, Pageable pageable) {
        return searchStrategy.search(searchRequest, pageable);
    }

    /**
     * 테스트 용도로 검색 결과만 단순히 리턴하는 메서드
     * @param searchRequest
     * @param pageable
     * @return
     */
    public List<SearchHitResult> search(SearchRequest searchRequest, Pageable pageable) {
        return searchPage(searchRequest, pageable).results();
    }

    /**
     * 테스트 용도로 첫페이지 검색 결과만 단순히 리턴하는 메서드
     * @param searchRequest
     * @param pageable
     * @return
     */
    public List<SearchHitResult> search(String query, int size) {
        SearchRequest request = new SearchRequest(query, null, null, null);
        return searchPage(request, SearchPagingPolicy.toPageable(SearchPagingPolicy.DEFAULT_PAGE, size)).results();
    }
}
