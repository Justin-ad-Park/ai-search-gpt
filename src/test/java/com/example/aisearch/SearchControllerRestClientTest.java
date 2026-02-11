package com.example.aisearch;

import com.example.aisearch.service.indexing.bootstrap.IndexManagementService;
import com.example.aisearch.service.indexing.bootstrap.ProductIndexingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchControllerRestClientTest {

    @LocalServerPort
    private int port;

    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private ProductIndexingService productIndexingService;

    @BeforeAll
    void setUp() {
        indexManagementService.recreateIndex();
        productIndexingService.reindexData();
    }

    @Test
    void searchShouldReturnResults() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create("http://localhost:" + port + "/api/search?q=어린이%20간식");
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[REST_CLIENT] status=" + response.statusCode());
        System.out.println("[REST_CLIENT] body=" + response.body());

        assertEquals(200, response.statusCode());
    }
}
