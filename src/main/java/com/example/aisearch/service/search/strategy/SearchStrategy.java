package com.example.aisearch.service.search.strategy;

import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.SearchRequest;
import org.springframework.data.domain.Pageable;

public interface SearchStrategy {
    SearchPageResult search(SearchRequest searchRequest, Pageable pageable);
}
