package com.example.aisearch.service.search;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.service.search.strategy.SearchStrategy;
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

    public List<SearchHitResult> search(SearchRequest searchRequest) {
        return searchStrategy.search(searchRequest);
    }

    public List<SearchHitResult> search(String query, int size) {
        return search(new SearchRequest(query, size, null, null));
    }
}
