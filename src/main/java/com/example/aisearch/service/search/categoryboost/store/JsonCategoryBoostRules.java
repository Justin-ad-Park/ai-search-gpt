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

/**
 * category_boosting.json 기반 룰 저장소 구현체.
 * 조회 계약(CategoryBoostRules)과 재로딩 계약(CategoryBoostRulesReloader)을 함께 제공한다.
 */
@Component
public class JsonCategoryBoostRules implements CategoryBoostRules, CategoryBoostRulesReloader {

    private static final Logger log = LoggerFactory.getLogger(JsonCategoryBoostRules.class);
    private static final String RULE_FILE_PATH = "classpath:data/category_boosting.json";
    private static final String VERSION_CHECK_GATE_KEY = "version-check-gate";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private volatile String ruleFilePath;
    private final Cache<String, Boolean> versionCheckGate;
    // JsonCategoryBoostRules_AtomicReference.md 문서 참고
    private final AtomicReference<CategoryBoostCacheEntry> currentEntry;

    // 스프링 빈 생성용 기본 생성자:
    // 운영 기본 룰 경로(classpath:data/category_boosting.json)와
    // application.yaml의 TTL 설정값(category-boost-cache-ttl-seconds)을 사용한다.
    @Autowired
    public JsonCategoryBoostRules(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            AiSearchProperties properties
    ) {
        this(resourceLoader, objectMapper, RULE_FILE_PATH, properties.categoryBoostCacheTtlSeconds());
    }

    // 테스트/수동 구성용 생성자:
    // 호출자가 룰 파일 경로와 TTL을 직접 지정할 수 있다.
    public JsonCategoryBoostRules(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            String ruleFilePath,
            long cacheTtlSeconds
    ) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.ruleFilePath = ruleFilePath;
        // 룰 파일 버전을 매 호출마다 확인하면 I/O 비용이 커지므로 TTL 동안 버전 체크를 생략한다.
        this.versionCheckGate = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofSeconds(Math.max(1L, cacheTtlSeconds)))
                .build();
        this.currentEntry = new AtomicReference<>(CategoryBoostCacheEntry.empty());
        loadInitialRules();
    }

    // 테스트나 운영 제어 코드에서 룰 경로를 교체할 때 사용한다.
    // 경로 변경 직후 다음 조회/재로딩에서 새 파일을 반영하도록 version check gate를 비운다.
    void setRuleFilePath(String ruleFilePath) {
        this.ruleFilePath = ruleFilePath;
        this.versionCheckGate.invalidate(VERSION_CHECK_GATE_KEY);
    }

    /**
     *
     * @param keyword 정규화된 검색어(trim 이후)
     * @return
     * Map<String, Double>
     *     String : 카테고리 ID
     *      Double : 가중치 (예: 0.2)
     */
    @Override
    public Optional<Map<String, Double>> findByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        // TTL이 만료된 시점에만 버전 변경 여부를 확인해 필요 시 재로딩한다.
        refreshIfNeeded();
        Map<String, Double> boosts = currentEntry.get().rulesByKeyword().get(keyword);
        return boosts == null ? Optional.empty() : Optional.of(boosts);
    }

    @Override
    public void reload() {
        synchronized (this) {
            // 운영 중 수동 reload 요청 시 즉시 버전 확인/재로딩을 시도한다.
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
            // 초기 로딩 실패로 서비스 전체가 죽지 않도록 빈 룰로 시작한다.
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
            // version 값이 바뀐 경우에만 전체 룰을 다시 읽어 캐시를 교체한다.
            if (!Objects.equals(cached.version(), newVersion)) {
                currentEntry.set(loadAll(path));
            }
            return true;
        } catch (Exception e) {
            // 재로딩 중 오류가 나도 기존 캐시를 유지해 검색 품질 급락을 방지한다.
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
            // 유효한 keyword + boost 맵이 모두 있을 때만 룰로 채택한다.
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
            // JSON 객체 키와 Painless params 맵 조회를 맞추기 위해 카테고리 ID를 문자열 키로 유지한다.
            normalized.put(key.trim(), value);
        }
        return Map.copyOf(normalized);
    }

    private String currentRulePath() {
        String path = ruleFilePath;
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
