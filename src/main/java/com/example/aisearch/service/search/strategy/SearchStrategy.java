package com.example.aisearch.service.search.strategy;

import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.SearchRequest;

public interface SearchStrategy {
    SearchPageResult search(SearchRequest searchRequest);
}
