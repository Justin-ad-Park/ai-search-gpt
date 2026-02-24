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
        // CATEGORY_BOOSTING_DESC 요청이 아니면 기존 정렬을 그대로 사용한다.
        if (request.sortOption() != SearchSortOption.CATEGORY_BOOSTING_DESC) {
            return CategoryBoostingDecision.noBoost(request.sortOption());
        }

        // query가 없으면 부스팅 조건을 만족할 수 없으므로 RELEVANCE_DESC로 fallback 한다.
        String query = request.query();
        if (query == null || query.isBlank()) {
            return CategoryBoostingDecision.noBoost(SearchSortOption.RELEVANCE_DESC);
        }

        // trim+equals 기반으로 룰 키워드를 조회한다.
        Optional<Map<String, Double>> boostByKeyword = ruleSource.findBoostByKeyword(query);
        if (boostByKeyword.isEmpty()) {
            return CategoryBoostingDecision.noBoost(SearchSortOption.RELEVANCE_DESC);
        }

        return CategoryBoostingDecision.apply(boostByKeyword.get());
    }
}
