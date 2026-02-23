package com.example.aisearch.service.search.boosting;

import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryBoostingPolicyResolverTest {

    @Test
    void shouldNotApplyBoostWhenSortIsNotCategoryBoosting() {
        CategoryBoostingRuleSource ruleSource = mock(CategoryBoostingRuleSource.class);
        CategoryBoostingPolicyResolver resolver = new CategoryBoostingPolicyResolver(ruleSource);

        SearchRequest request = new SearchRequest("간식", null, null, SearchSortOption.PRICE_ASC);
        CategoryBoostingDecision decision = resolver.resolve(request);

        assertFalse(decision.applyCategoryBoost());
        assertEquals(SearchSortOption.PRICE_ASC, decision.effectiveSortOption());
        assertEquals(Map.of(), decision.categoryBoostById());
    }

    @Test
    void shouldApplyBoostWhenKeywordMatchesExactly() {
        CategoryBoostingRuleSource ruleSource = mock(CategoryBoostingRuleSource.class);
        when(ruleSource.findBoostByKeyword("간식")).thenReturn(Optional.of(Map.of("1", 0.2)));
        CategoryBoostingPolicyResolver resolver = new CategoryBoostingPolicyResolver(ruleSource);

        SearchRequest request = new SearchRequest("  간식  ", null, null, SearchSortOption.CATEGORY_BOOSTING_DESC);
        CategoryBoostingDecision decision = resolver.resolve(request);

        assertTrue(decision.applyCategoryBoost());
        assertEquals(SearchSortOption.CATEGORY_BOOSTING_DESC, decision.effectiveSortOption());
        assertEquals(0.2, decision.categoryBoostById().get("1"));
    }

    @Test
    void shouldFallbackToRelevanceWhenKeywordDoesNotMatch() {
        CategoryBoostingRuleSource ruleSource = mock(CategoryBoostingRuleSource.class);
        when(ruleSource.findBoostByKeyword("오징어 튀김")).thenReturn(Optional.empty());
        CategoryBoostingPolicyResolver resolver = new CategoryBoostingPolicyResolver(ruleSource);

        SearchRequest request = new SearchRequest("오징어 튀김", null, null, SearchSortOption.CATEGORY_BOOSTING_DESC);
        CategoryBoostingDecision decision = resolver.resolve(request);

        assertFalse(decision.applyCategoryBoost());
        assertEquals(SearchSortOption.RELEVANCE_DESC, decision.effectiveSortOption());
        assertEquals(Map.of(), decision.categoryBoostById());
    }

    @Test
    void shouldFallbackToRelevanceWhenQueryIsBlank() {
        CategoryBoostingRuleSource ruleSource = mock(CategoryBoostingRuleSource.class);
        CategoryBoostingPolicyResolver resolver = new CategoryBoostingPolicyResolver(ruleSource);

        SearchRequest request = new SearchRequest("   ", null, null, SearchSortOption.CATEGORY_BOOSTING_DESC);
        CategoryBoostingDecision decision = resolver.resolve(request);

        assertFalse(decision.applyCategoryBoost());
        assertEquals(SearchSortOption.RELEVANCE_DESC, decision.effectiveSortOption());
        assertEquals(Map.of(), decision.categoryBoostById());
    }
}
