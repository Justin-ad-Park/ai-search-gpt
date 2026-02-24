package com.example.aisearch.service.search.categoryboost.store;

import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.service.search.categoryboost.api.CategoryBoostRules;
import com.example.aisearch.service.search.categoryboost.api.CategoryBoostRulesReloader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class JsonCategoryBoostRules implements CategoryBoostRules, CategoryBoostRulesReloader {

    private static final Logger log = LoggerFactory.getLogger(JsonCategoryBoostRules.class);
    private static final String RULE_FILE_PATH = "classpath:data/category_boosting.json";
    private static final String VERSION_CHECK_GATE_KEY = "version-check-gate";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final Supplier<String> ruleFilePathSupplier;
    private final Cache<String, Boolean> versionCheckGate;
    private final AtomicReference<CategoryBoostCacheEntry> currentEntry;

    @Autowired
    public JsonCategoryBoostRules(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            AiSearchProperties properties
    ) {
        this(resourceLoader, objectMapper, () -> RULE_FILE_PATH, properties.categoryBoostCacheTtlSeconds());
    }

    public JsonCategoryBoostRules(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            String ruleFilePath,
            long cacheTtlSeconds
    ) {
        this(resourceLoader, objectMapper, () -> ruleFilePath, cacheTtlSeconds);
    }

    JsonCategoryBoostRules(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            Supplier<String> ruleFilePathSupplier,
            long cacheTtlSeconds
    ) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.ruleFilePathSupplier = ruleFilePathSupplier;
        this.versionCheckGate = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofSeconds(Math.max(1L, cacheTtlSeconds)))
                .build();
        this.currentEntry = new AtomicReference<>(CategoryBoostCacheEntry.empty());
        loadInitialRules();
    }

    @Override
    public Optional<Map<String, Double>> findByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        refreshIfNeeded();
        Map<String, Double> boosts = currentEntry.get().rulesByKeyword().get(keyword);
        return boosts == null ? Optional.empty() : Optional.of(boosts);
    }

    @Override
    public void reload() {
        synchronized (this) {
            if (checkAndReloadIfVersionChanged()) {
                versionCheckGate.put(VERSION_CHECK_GATE_KEY, Boolean.TRUE);
            }
        }
    }

    private void loadInitialRules() {
        String path = currentRulePath();
        try {
            currentEntry.set(loadAll(path));
            versionCheckGate.put(VERSION_CHECK_GATE_KEY, Boolean.TRUE);
        } catch (Exception e) {
            log.warn("카테고리 부스팅 초기 룰 로딩 실패. 빈 룰로 동작합니다. path={}", path, e);
        }
    }

    private void refreshIfNeeded() {
        if (versionCheckGate.getIfPresent(VERSION_CHECK_GATE_KEY) != null) {
            return;
        }
        synchronized (this) {
            if (versionCheckGate.getIfPresent(VERSION_CHECK_GATE_KEY) != null) {
                return;
            }
            if (checkAndReloadIfVersionChanged()) {
                versionCheckGate.put(VERSION_CHECK_GATE_KEY, Boolean.TRUE);
            }
        }
    }

    private boolean checkAndReloadIfVersionChanged() {
        String path = currentRulePath();
        try {
            String newVersion = readVersion(path);
            CategoryBoostCacheEntry cached = currentEntry.get();
            if (!Objects.equals(cached.version(), newVersion)) {
                currentEntry.set(loadAll(path));
            }
            return true;
        } catch (Exception e) {
            log.warn("카테고리 부스팅 룰 버전 확인/재로딩 실패. 기존 캐시를 유지합니다. path={}", path, e);
            return false;
        }
    }

    private CategoryBoostCacheEntry loadAll(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            CategoryBoostingConfig config = objectMapper.readValue(inputStream, CategoryBoostingConfig.class);
            if (config == null || config.version() == null || config.version().isBlank()) {
                throw new IllegalStateException("category_boosting.json version 값이 유효하지 않습니다. path=" + path);
            }
            return new CategoryBoostCacheEntry(config.version().trim(), toRuleMap(config.rules()));
        }
    }

    private String readVersion(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode versionNode = root == null ? null : root.get("version");
            String version = versionNode == null ? "" : versionNode.asText("");
            if (version.isBlank()) {
                throw new IllegalStateException("category_boosting.json version 값이 비어 있습니다. path=" + path);
            }
            return version.trim();
        }
    }

    private Map<String, Map<String, Double>> toRuleMap(List<CategoryBoostingRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Map.of();
        }
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

    private String currentRulePath() {
        String path = ruleFilePathSupplier.get();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("카테고리 부스팅 룰 파일 경로가 비어 있습니다.");
        }
        return path;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CategoryBoostingConfig(
            String version,
            List<CategoryBoostingRule> rules
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CategoryBoostingRule(
            String keyword,
            Map<String, Double> categoryBoostById
    ) {
    }

    private record CategoryBoostCacheEntry(
            String version,
            Map<String, Map<String, Double>> rulesByKeyword
    ) {
        private static CategoryBoostCacheEntry empty() {
            return new CategoryBoostCacheEntry("", Map.of());
        }
    }
}
