package com.example.aisearch;

import com.example.aisearch.service.indexing.bootstrap.IndexManagementService;
import com.example.aisearch.service.indexing.bootstrap.ProductIndexingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SynonymsRestClientIntegrationTest extends TruststoreTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private ProductIndexingService productIndexingService;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void setUp() {
        indexManagementService.recreateIndex();
        productIndexingService.reindexData();
    }

    @AfterEach
    void restoreProductionSynonyms() throws Exception {
        try {
            reloadSynonyms("PRODUCTION");
        } catch (Exception ignored) {
            // 테스트 본문의 실패 원인을 가리지 않기 위해 정리 단계 예외는 무시
        }
    }

    @Test
    void regressionSynonymsShouldMakeTteokgukQueryReturnSaengMandu() throws Exception {
        HttpResponse<String> reloadResponse = reloadSynonyms("REGRESSION");
        assertEquals(200, reloadResponse.statusCode());

        JsonNode reloadJson = objectMapper.readTree(reloadResponse.body());
        assertTrue(reloadJson.path("updated").asBoolean(false));
        assertTrue(reloadJson.path("reloaded").asBoolean(false));
        assertEquals("REGRESSION", reloadJson.path("mode").asText());

        HttpResponse<String> searchResponse = search("떡국", 20);
        assertEquals(200, searchResponse.statusCode());

        JsonNode searchJson = objectMapper.readTree(searchResponse.body());
        JsonNode results = searchJson.path("results");
        assertTrue(results.isArray());

        boolean containsSaengMandu = false;
        for (JsonNode hit : results) {
            String name = hit.path("source").path("product_name").asText("");
            if (name.contains("생만두")) {
                containsSaengMandu = true;
                break;
            }
        }
        assertTrue(containsSaengMandu, "회귀 동의어 적용 후 '떡국' 검색에 생만두 상품이 포함되어야 합니다.");
    }

    private HttpResponse<String> reloadSynonyms(String mode) throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/api/search/reload-synonyms");
        String body = """
                {
                  "mode": "%s"
                }
                """.formatted(mode);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> search(String query, int size) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create("http://localhost:" + port + "/api/search?q=" + encodedQuery + "&page=1&size=" + size + "&sort=RELEVANCE_DESC");
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
