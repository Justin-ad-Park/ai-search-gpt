package com.example.aisearch.service.search.strategy;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchRequest;

import java.util.List;

public interface SearchStrategy {
    List<SearchHitResult> search(SearchRequest searchRequest);
}
