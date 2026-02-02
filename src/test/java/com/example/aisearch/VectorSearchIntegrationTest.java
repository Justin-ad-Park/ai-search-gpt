package com.example.aisearch;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.service.VectorIndexService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VectorSearchIntegrationTest {

    private static Process portForwardProcess;

    static {
        setupForDirectExecution();
    }

    @Autowired
    private VectorIndexService vectorIndexService;

    @Test
    @Order(1)
    void reindexSampleData() {
        vectorIndexService.recreateIndex();
        long indexed = vectorIndexService.reindexSampleData();
        Assertions.assertTrue(indexed >= 100, "최소 100건 이상 인덱싱되어야 합니다.");
        System.out.println("[INDEXED] total=" + indexed);
    }

    @Test
    @Order(2)
    void semanticSearchShouldReturnRelevantProducts() {
        String query = "어린이가 먹을 만한 전통 스낵";
        String[] expectedCategoryKeywords = {"간식", "청과"};

        assertSemanticSearchContainsCategories(query, 5, expectedCategoryKeywords);
    }

    @Test
    @Order(3)
    void semanticSearchShouldReturnRelevantProducts2() {
        String query = "바다 식품";
        String[] expectedCategoryKeywords = {"수산물"};

        assertSemanticSearchContainsCategories(query, 20, expectedCategoryKeywords);
    }

    @Test
    @Order(4)
    void semanticSearchShouldReturnEmptyWhenBelowThreshold() {
        String query = "태풍";

        List<SearchHitResult> results = vectorIndexService.search(query, 5);

        System.out.println("[SEARCH] query=" + query);
        results.forEach(hit -> System.out.printf(
                "rank=?, score=%s, id=%s, name=%s, category=%s%n",
                hit.score(),
                hit.id(),
                hit.source().get("product_name"),
                hit.source().get("category")
        ));

        Assertions.assertTrue(results.isEmpty(), "MIN_SCORE_THRESHOLD 이하이면 검색 결과가 없어야 합니다.");
    }

    private void assertSemanticSearchContainsCategories(String query, int size, String... expectedCategoryKeywords) {
        List<SearchHitResult> results = vectorIndexService.search(query, size);

        System.out.println("[SEARCH] query=" + query);
        for (int i = 0; i < results.size(); i++) {
            SearchHitResult hit = results.get(i);
            System.out.println("rank=" + (i + 1)
                    + ", score=" + hit.score()
                    + ", id=" + hit.id()
                    + ", name=" + hit.source().get("product_name")
                    + ", category=" + hit.source().get("category"));
        }

        Assertions.assertFalse(results.isEmpty(), "검색 결과가 비어있으면 안됩니다.");
        results.forEach(hit -> System.out.printf(
                "[CATEGORY_CHECK] category=%s, name=%s, score=%s%n",
                hit.source().get("category"),
                hit.source().get("product_name"),
                hit.score()
        ));
        boolean containsExpectedCategory = results.stream()
                .map(hit -> (String) hit.source().get("category"))
                .anyMatch(category -> containsAnyKeyword(category, expectedCategoryKeywords));
        Assertions.assertTrue(containsExpectedCategory, "상위 결과에 기대 카테고리가 포함되어야 합니다.");
    }

    private boolean containsAnyKeyword(String category, String... keywords) {
        if (category == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (category.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @AfterAll
    static void teardown() {
        if (portForwardProcess != null && portForwardProcess.isAlive()) {
            portForwardProcess.destroy();
        }
    }

    private static void setupForDirectExecution() {
        try {
            trustAllHttpsForTestOnly();

            setIfMissing("AI_SEARCH_ES_USERNAME", "elastic");

            if (isBlank(System.getProperty("AI_SEARCH_ES_PASSWORD")) && isBlank(System.getenv("AI_SEARCH_ES_PASSWORD"))) {
                String password = runCommand(
                        "kubectl", "get", "secret", "ai-search-es-es-elastic-user",
                        "-n", "ai-search", "-o", "go-template={{.data.elastic | base64decode}}"
                ).trim();
                setIfMissing("AI_SEARCH_ES_PASSWORD", password);
            }

            if (isBlank(System.getProperty("AI_SEARCH_ES_URL")) && isBlank(System.getenv("AI_SEARCH_ES_URL"))) {
                String service = findElasticsearchHttpService();
                portForwardProcess = new ProcessBuilder(
                        "kubectl", "port-forward", "-n", "ai-search", "service/" + service, "9200:9200"
                ).redirectErrorStream(true).start();
                Thread.sleep(3000L);
                setIfMissing("AI_SEARCH_ES_URL", "http://localhost:9200");
            }
        } catch (Exception e) {
            throw new RuntimeException("VectorSearchIntegrationTest 사전 설정 실패", e);
        }
    }

    private static String findElasticsearchHttpService() throws IOException, InterruptedException {
        String output = runCommand(
                "kubectl", "get", "svc", "-n", "ai-search",
                "-o", "jsonpath={range .items[*]}{.metadata.name}{\"\\n\"}{end}"
        );
        for (String line : output.split("\\R")) {
            if (line.endsWith("-es-http")) {
                return line.trim();
            }
        }
        return "ai-search-es-es-http";
    }

    private static void trustAllHttpsForTestOnly() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    private static void setIfMissing(String key, String value) {
        if (isBlank(System.getProperty(key)) && isBlank(System.getenv(key))) {
            System.setProperty(key, value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("command failed: " + String.join(" ", command) + "\n" + out);
        }
        return out.toString();
    }
}
