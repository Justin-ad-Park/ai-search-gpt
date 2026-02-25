package com.example.aisearch.service.search.categoryboost.policy;

import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import com.example.aisearch.service.search.categoryboost.api.CategoryBoostRules;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CategoryBoostingDeciderTest {

    @Test
    void shouldKeepOriginalSortWhenSortOptionIsNotCategoryBoosting() {
        CategoryBoostRules rules = keyword -> Optional.of(Map.of("1", 0.2));
        CategoryBoostingDecider decider = new CategoryBoostingDecider(rules);

        SearchRequest request = new SearchRequest("간식", null, null, SearchSortOption.PRICE_ASC);
        CategoryBoostingResult result = decider.decide(request);

        assertFalse(result.applyCategoryBoost());
        assertEquals(SearchSortOption.PRICE_ASC, result.searchSortOption());
    }

    @Test
    void shouldApplyBoostWhenKeywordMatches() {
        CategoryBoostRules rules = keyword -> "간식".equals(keyword)
                ? Optional.of(Map.of("1", 0.2))
                : Optional.empty();
        CategoryBoostingDecider decider = new CategoryBoostingDecider(rules);

        SearchRequest request = new SearchRequest("  간식  ", null, null, SearchSortOption.CATEGORY_BOOSTING_DESC);
        CategoryBoostingResult result = decider.decide(request);

        assertTrue(result.applyCategoryBoost());
        assertEquals(SearchSortOption.CATEGORY_BOOSTING_DESC, result.searchSortOption());
        assertEquals(0.2, result.categoryBoostById().get("1"));
    }

    @Test
    void shouldFallbackToRelevanceWhenKeywordDoesNotMatch() {
        CategoryBoostRules rules = keyword -> Optional.empty();
        CategoryBoostingDecider decider = new CategoryBoostingDecider(rules);

        SearchRequest request = new SearchRequest("오징어 튀김", null, null, SearchSortOption.CATEGORY_BOOSTING_DESC);
        CategoryBoostingResult result = decider.decide(request);

        assertFalse(result.applyCategoryBoost());
        assertEquals(SearchSortOption.RELEVANCE_DESC, result.searchSortOption());
    }
}
