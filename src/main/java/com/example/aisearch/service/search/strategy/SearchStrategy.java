package com.example.aisearch.service.search.strategy;

import com.example.aisearch.model.SearchHitResult;

import java.util.List;

public interface SearchStrategy {
    List<SearchHitResult> search(String query, int size);
}
