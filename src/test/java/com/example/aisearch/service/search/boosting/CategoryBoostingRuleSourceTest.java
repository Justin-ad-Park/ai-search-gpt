package com.example.aisearch.service.search.boosting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CategoryBoostingRuleSourceTest {

    @Test
    void shouldLoadCategoryBoostRulesFromJson() {
        CategoryBoostingRuleSource source = new CategoryBoostingRuleSource(
                new DefaultResourceLoader(),
                new ObjectMapper()
        );

        Optional<Map<String, Double>> snack = source.findBoostByKeyword("간식");
        assertTrue(snack.isPresent());
        assertEquals(0.20, snack.get().get("1"));
        assertEquals(0.10, snack.get().get("2"));
        assertEquals(0.10, snack.get().get("8"));
    }

    @Test
    void shouldReturnEmptyWhenKeywordNotExists() {
        CategoryBoostingRuleSource source = new CategoryBoostingRuleSource(
                new DefaultResourceLoader(),
                new ObjectMapper()
        );

        assertTrue(source.findBoostByKeyword("없는키워드").isEmpty());
    }
}
