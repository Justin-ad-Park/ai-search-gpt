package com.example.aisearch.service.search.boosting;

import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class CategoryBoostingPolicyResolver {

    private final CategoryBoostingRuleSource ruleSource;

    public CategoryBoostingPolicyResolver(CategoryBoostingRuleSource ruleSource) {
        this.ruleSource = ruleSource;
    }

    public CategoryBoostingDecision resolve(SearchRequest request) {
        if (request.sortOption() != SearchSortOption.CATEGORY_BOOSTING_DESC) {
            return CategoryBoostingDecision.noBoost(request.sortOption());
        }
        String query = request.query();
        if (query == null || query.isBlank()) {
            return CategoryBoostingDecision.noBoost(SearchSortOption.RELEVANCE_DESC);
        }
        Optional<Map<String, Double>> boostByKeyword = ruleSource.findBoostByKeyword(query);
        if (boostByKeyword.isEmpty()) {
            return CategoryBoostingDecision.noBoost(SearchSortOption.RELEVANCE_DESC);
        }
        return CategoryBoostingDecision.apply(boostByKeyword.get());
    }
}
