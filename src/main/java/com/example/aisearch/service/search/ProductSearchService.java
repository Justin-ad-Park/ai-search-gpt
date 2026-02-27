package com.example.aisearch.service.search;

import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.ProductSearchRequest;
import com.example.aisearch.service.search.strategy.SearchStrategy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchService {

    private final SearchStrategy searchStrategy;

    public ProductSearchService(
            SearchStrategy searchStrategy
    ) {
        this.searchStrategy = searchStrategy;
    }

    public SearchPageResult searchPage(ProductSearchRequest searchRequest, Pageable pageable) {
        return searchStrategy.search(searchRequest, pageable);
    }
}
