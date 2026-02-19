package com.example.aisearch.model.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchRequestTest {

    @Test
    void shouldTrimQueryAndDefaultSortOption() {
        SearchRequest request = new SearchRequest("  간식  ", null, null, null);

        assertEquals("간식", request.query());
        assertEquals(SearchSortOption.RELEVANCE_DESC, request.sortOption());
        assertTrue(request.hasQuery());
    }

    @Test
    void shouldUseExplicitSortOption() {
        SearchRequest request = new SearchRequest("간식", null, null, SearchSortOption.PRICE_ASC);
        assertEquals(SearchSortOption.PRICE_ASC, request.sortOption());
    }

    @Test
    void shouldTreatBlankQueryAsOptional() {
        SearchRequest request = new SearchRequest("   ", null, List.of(1, 2, 2), null);

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

    @Test
    void shouldRejectInvalidPageSizePolicy() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SearchPagingPolicy.toPageable(0, 5)
        );
        assertTrue(ex.getMessage().contains("page"));
    }
}
