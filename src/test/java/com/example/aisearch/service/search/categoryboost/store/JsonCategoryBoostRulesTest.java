package com.example.aisearch.service.search.categoryboost.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JsonCategoryBoostRulesTest {

    @Test
    void shouldLoadRulesFromFixtureV1() {
        JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
                new DefaultResourceLoader(),
                new ObjectMapper(),
                "classpath:data/category_boosting_v1.json",
                300
        );

        Optional<Map<String, Double>> apple = rules.findByKeyword("사과");
        assertTrue(apple.isPresent());
        assertEquals(0.20, apple.get().get("4"));
    }

    @Test
    void shouldReloadRulesWhenVersionChangesByPathSwitching() {
        AtomicReference<String> pathRef = new AtomicReference<>("classpath:data/category_boosting_v1.json");
        JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
                new DefaultResourceLoader(),
                new ObjectMapper(),
                pathRef::get,
                300
        );

        assertEquals(0.20, rules.findByKeyword("사과").orElseThrow().get("4"));

        pathRef.set("classpath:data/category_boosting_v2.json");
        rules.reload();

        assertEquals(0.30, rules.findByKeyword("사과").orElseThrow().get("4"));
    }
}
