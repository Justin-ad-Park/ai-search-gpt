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

    public List<SearchHitResult> search(SearchRequest searchRequest, Pageable pageable) {
        return searchPage(searchRequest, pageable).results();
    }

    public List<SearchHitResult> search(String query, int size) {
        SearchRequest request = new SearchRequest(query, null, null, null);
        return searchPage(request, SearchPagingPolicy.toPageable(SearchPagingPolicy.DEFAULT_PAGE, size)).results();
    }
}
