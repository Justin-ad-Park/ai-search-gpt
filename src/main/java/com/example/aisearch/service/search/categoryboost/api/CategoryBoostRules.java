package com.example.aisearch.service.search.categoryboost.api;

import java.util.Map;
import java.util.Optional;

public interface CategoryBoostRules {
    Optional<Map<String, Double>> findByKeyword(String keyword);
}
