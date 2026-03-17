package com.example.aisearch;

import com.example.aisearch.service.indexing.orchestration.IndexRolloutService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ai-search.index-name=search-rest-it-products",
                "ai-search.read-alias=search-rest-it-products-read",
                "ai-search.synonyms-set=search-rest-it-synonyms"
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchControllerRestClientTest extends RestApiIntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private IndexRolloutService indexRolloutService;

    @BeforeAll
    void setUp() throws Exception {
        printIsolationConfig("SearchControllerRestClientTest");
        deleteAllVersionedIndices();
        indexRolloutService.rollOutFromSourceData();
    }

    @AfterAll
    void tearDown() throws Exception {
        deleteAllVersionedIndices();
    }

    @Test
    void searchShouldReturnResults() throws Exception {
        JsonNode response = getJsonAndAssertOk("/api/search?q=어린이%20간식");

        System.out.println("[REST_CLIENT] totalElements=" + response.path("totalElements").asLong());
        System.out.println("[REST_CLIENT] count=" + response.path("count").asInt());

        assertTrue(response.path("results").isArray());
        assertTrue(response.path("count").asInt() > 0, "검색 결과가 1건 이상이어야 합니다.");
    }

    @Override
    protected int port() {
        return port;
    }
}
