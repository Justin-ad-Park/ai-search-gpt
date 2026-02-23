package com.example.aisearch.service.search.boosting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CategoryBoostingRuleSource {

    private static final Logger log = LoggerFactory.getLogger(CategoryBoostingRuleSource.class);
    private static final String RULE_FILE_PATH = "classpath:data/category_boosting.json";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Double>> rulesByKeyword;

    public CategoryBoostingRuleSource(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.rulesByKeyword = loadRulesSafely();
    }

    public Optional<Map<String, Double>> findBoostByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        Map<String, Double> boosts = rulesByKeyword.get(keyword);
        return boosts == null ? Optional.empty() : Optional.of(boosts);
    }

    private Map<String, Map<String, Double>> loadRulesSafely() {
        try {
            return loadRules();
        } catch (Exception e) {
            log.warn("카테고리 부스팅 룰 로딩 실패. 빈 룰로 동작합니다. path={}", RULE_FILE_PATH, e);
            return Map.of();
        }
    }

    private Map<String, Map<String, Double>> loadRules() throws IOException {
        Resource resource = resourceLoader.getResource(RULE_FILE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            CategoryBoostingConfig config = objectMapper.readValue(inputStream, CategoryBoostingConfig.class);
            return toRuleMap(config == null ? List.of() : config.rules());
        }
    }

    private Map<String, Map<String, Double>> toRuleMap(List<CategoryBoostingRule> rules) {
        Map<String, Map<String, Double>> ruleMap = new LinkedHashMap<>();
        for (CategoryBoostingRule rule : rules) {
            if (rule == null || rule.keyword() == null) {
                continue;
            }
            String keyword = rule.keyword().trim();
            if (keyword.isBlank()) {
                continue;
            }
            Map<String, Double> boosts = normalizeBoostMap(rule.categoryBoostById());
            if (boosts.isEmpty()) {
                continue;
            }
            ruleMap.put(keyword, boosts);
        }
        return Map.copyOf(ruleMap);
    }

    private Map<String, Double> normalizeBoostMap(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            normalized.put(key.trim(), value);
        }
        return Map.copyOf(normalized);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CategoryBoostingConfig(List<CategoryBoostingRule> rules) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CategoryBoostingRule(String keyword, Map<String, Double> categoryBoostById) {
    }
}
