package com.example.aisearch.integration.synonyms;

import com.example.aisearch.integration.helper.RestApiIntegrationTestBase;
import com.example.aisearch.service.indexing.orchestration.IndexRolloutService;
import com.example.aisearch.service.synonym.SynonymReloadMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ai-search.index-name=synonym-rest-it-products",
                "ai-search.read-alias=synonym-rest-it-products-read",
                "ai-search.synonyms-set=synonym-rest-it-synonyms",
                "ai-search.min-score-threshold=0.0"
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SynonymRestApiIntegrationTestBase extends RestApiIntegrationTestBase {

    private static final String SEARCH_ANALYZER = "ko_mall_search_analyzer";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    protected IndexRolloutService indexRolloutService;

    @BeforeAll
    void setUpSynonymFixture() throws Exception {
        printIsolationConfig(getClass().getSimpleName());
        deleteAllVersionedIndices();
        indexRolloutService.rollOutFromSourceData();
    }

    @AfterAll
    void restoreProductionSynonyms() throws Exception {
        try {
            reloadSynonyms(SynonymReloadMode.PRODUCTION);
        } catch (Exception ignored) {
            // 테스트 본문의 실패 원인을 가리지 않기 위해 정리 단계 예외는 무시
        }
        deleteAllVersionedIndices();
    }

    protected JsonNode reloadSynonymsAndAssert(SynonymReloadMode mode) throws Exception {
        HttpResponse<String> reloadResponse = reloadSynonyms(mode);
        assertEquals(200, reloadResponse.statusCode());
        JsonNode reloadJson = readJson(reloadResponse);
        assertTrue(reloadJson.path("updated").asBoolean(false));
        assertTrue(reloadJson.path("reloaded").asBoolean(false));
        assertEquals(mode.name(), reloadJson.path("mode").asText());
        return reloadJson;
    }

    protected JsonNode searchAndAssertOk(String query, int size) throws Exception {
        HttpResponse<String> searchResponse = search(query, size);
        assertEquals(200, searchResponse.statusCode());
        JsonNode searchJson = readJson(searchResponse);
        assertTrue(searchJson.path("results").isArray());
        return searchJson;
    }

    protected JsonNode analyzeAndAssertOk(String text) throws Exception {
        return OBJECT_MAPPER.readTree(analyze(text));
    }

    protected List<String> extractFinalTokens(JsonNode analyzeJson) {
        List<String> tokens = new ArrayList<>();
        JsonNode detail = analyzeJson.path("detail");
        JsonNode tokenFilters = detail.path("tokenfilters");
        JsonNode finalStageTokens = tokenFilters.isArray() && tokenFilters.size() > 0
                ? tokenFilters.get(tokenFilters.size() - 1).path("tokens")
                : analyzeJson.path("tokens");
        if (!finalStageTokens.isArray()) {
            return tokens;
        }
        for (JsonNode token : finalStageTokens) {
            tokens.add(token.path("token").asText());
        }
        return tokens;
    }

    protected void assertContainsProductName(JsonNode results, String expectedNameKeyword) {
        boolean containsExpectedProduct = false;
        for (JsonNode hit : results) {
            String name = hit.path("source").path("goods_name").asText("");
            if (name.contains(expectedNameKeyword)) {
                containsExpectedProduct = true;
                break;
            }
        }
        assertTrue(containsExpectedProduct, "검색 결과에 '" + expectedNameKeyword + "' 상품이 포함되어야 합니다.");
    }

    protected void assertContainsToken(List<String> tokens, String expectedToken) {
        assertTrue(tokens.contains(expectedToken),
                "analyze 결과에 토큰 '" + expectedToken + "' 이 포함되어야 합니다. actual=" + tokens);
    }

    protected long printSearchResults(String query, JsonNode searchJson, String targetKeyword) {
        JsonNode results = searchJson.path("results");
        System.out.println("[SYNONYM_SEARCH] query=" + query
                + ", totalElements=" + searchJson.path("totalElements").asLong()
                + ", totalPages=" + searchJson.path("totalPages").asInt()
                + ", count=" + searchJson.path("count").asInt()
                + ", targetKeyword=" + targetKeyword);

        long printed = java.util.stream.IntStream.range(0, results.size())
                .mapToObj(i -> new HitView(i, results.get(i)))
                .filter(hitView -> hitView.productName().contains(targetKeyword))
                .peek(hitView -> System.out.println("rank=" + hitView.rank()
                        + ", score=" + hitView.hit().path("score").asDouble()
                        + ", id=" + hitView.hit().path("id").asText()
                        + ", name=" + hitView.productName()
                        + ", category=" + hitView.source().path("lev3_category_id_name").asText()
                        + ", price=" + hitView.source().path("sale_price").asText()))
                .count();

        if (printed == 0) {
            System.out.println("[SYNONYM_SEARCH] target keyword not found in printed results");
        }

        return printed;
    }

    private HttpResponse<String> reloadSynonyms(SynonymReloadMode mode) throws Exception {
        String body = """
                {
                  "mode": "%s"
                }
                """.formatted(mode.name());
        return postJson("/api/search/reload-synonyms", body);
    }

    private HttpResponse<String> search(String query, int size) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return get("/api/search?q=" + encodedQuery + "&page=1&size=" + size + "&sort=RELEVANCE_DESC");
    }

    private String analyze(String text) throws Exception {
        return esClient.indices()
                .analyze(request -> request
                        .index(properties.readAlias())
                        .analyzer(SEARCH_ANALYZER)
                        .text(text)
                )
                .toString();
    }

    @Override
    protected int port() {
        return port;
    }

    private record HitView(int index, JsonNode hit) {
        int rank() {
            return index + 1;
        }

        JsonNode source() {
            return hit.path("source");
        }

        String productName() {
            return source().path("goods_name").asText("");
        }
    }
}
