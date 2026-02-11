package com.example.aisearch.service;

import com.example.aisearch.model.SearchHitResult;
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

    public List<SearchHitResult> search(String query, int size) {
        return searchStrategy.search(query, size);
    }
}
