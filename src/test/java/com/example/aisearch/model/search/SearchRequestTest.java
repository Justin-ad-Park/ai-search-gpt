package com.example.aisearch.model.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchRequestTest {

    @Test
    void shouldUseDefaultSizeAndTrimQuery() {
        SearchRequest request = new SearchRequest("  간식  ", null, null, null);

        assertEquals("간식", request.query());
        assertEquals(SearchRequest.DEFAULT_SIZE, request.size());
        assertEquals(SearchSortOption.RELEVANCE_DESC, request.sortOption());
        assertTrue(request.hasQuery());
    }

    @Test
    void shouldUseExplicitSortOption() {
        SearchRequest request = new SearchRequest("간식", 5, null, null, SearchSortOption.PRICE_ASC);
        assertEquals(SearchSortOption.PRICE_ASC, request.sortOption());
    }

    @Test
    void shouldTreatBlankQueryAsOptional() {
        SearchRequest request = new SearchRequest("   ", 10, null, List.of(1, 2, 2));

        assertNull(request.query());
        assertFalse(request.hasQuery());
        assertEquals(List.of(1, 2), request.categoryIds());
        assertTrue(request.hasCategoryCondition());
    }

    @Test
    void shouldRejectInvalidPriceRange() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SearchPrice(10000, 1000)
        );
        assertTrue(ex.getMessage().contains("minPrice"));
    }
}
