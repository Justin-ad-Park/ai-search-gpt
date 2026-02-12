package com.example.aisearch;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchPrice;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.service.indexing.bootstrap.IndexManagementService;
import com.example.aisearch.service.indexing.bootstrap.ProductIndexingService;
import com.example.aisearch.service.search.VectorSearchService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VectorSearchIntegrationTest extends TruststoreTestBase {


    @Autowired
    private IndexManagementService indexManagementService;

    @Autowired
    private ProductIndexingService productIndexingService;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Test
    @Order(1)
    void reindexSampleData() {
        // 인덱스 재생성 후 샘플 데이터 인덱싱
        indexManagementService.recreateIndex();
        long indexed = productIndexingService.reindexData();
        Assertions.assertTrue(indexed >= 100, "최소 100건 이상 인덱싱되어야 합니다.");
        System.out.println("[INDEXED] total=" + indexed);
    }

    @Test
    @Order(2)
    void semanticSearchShouldReturnRelevantProducts() {
        // 아이 간식 관련 쿼리 테스트
        String query = "어린이가 먹기 좋은 건강한 간식";
        String[] expectedCategoryKeywords = {"간식"};

        assertSemanticSearchContainsCategories(query, 5, expectedCategoryKeywords);
    }

    @Test
    @Order(3)
    void semanticSearchShouldReturnRelevantProducts2() {
        // 수산물 관련 쿼리 테스트
        String query = "바다 가득한 ";
        String[] expectedCategoryKeywords = {"수산","간편식"};

        assertSemanticSearchContainsCategories(query, 20, expectedCategoryKeywords);
    }

    @Test
    @Order(4)
    void semanticSearchShouldReturnEmptyWhenBelowThreshold() {
        // 관련성이 낮은 키워드는 결과가 비어야 함
        String query = "태풍";

        List<SearchHitResult> results = vectorSearchService.search(query, 5);

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

    @Test
    @Order(5)
    void semanticSearchFiveQueriesTop5() {
        // 모델 비교용 5개 쿼리 결과를 출력
        String[] queries = {
                "어린이 간식으로 좋은 전통 과자",
                "다이어트에 좋은 저당 간식",
                "단백질 많은 간편식",
                "신선한 해산물 반찬",
                "채식 위주의 건강식"
        };

        int size = 5;
        for (String query : queries) {
            List<SearchHitResult> results = vectorSearchService.search(query, size);
            System.out.println("[COMPARE_QUERY] " + query);
            for (int i = 0; i < results.size(); i++) {
                SearchHitResult hit = results.get(i);
                System.out.println("rank=" + (i + 1)
                        + ", score=" + hit.score()
                        + ", id=" + hit.id()
                        + ", name=" + hit.source().get("product_name")
                        + ", category=" + hit.source().get("category"));
            }
        }
    }

    @Test
    @Order(6)
    void categoryFilterShouldReturnOnlyRequestedCategories() {
        SearchRequest request = new SearchRequest(null, 10, null, List.of(1, 2, 3));
        List<SearchHitResult> results = vectorSearchService.search(request);

        System.out.println("[CATEGORY_FILTER] categories=1,2,3");
        results.forEach(hit -> System.out.printf(
                "id=%s, name=%s, categoryId=%s, price=%s%n",
                hit.id(),
                hit.source().get("product_name"),
                hit.source().get("categoryId"),
                hit.source().get("price")
        ));

        Assertions.assertFalse(results.isEmpty(), "카테고리 필터 결과는 비어있으면 안 됩니다.");
        Assertions.assertTrue(results.stream().allMatch(hit -> {
            Integer categoryId = asInteger(hit.source(), "categoryId");
            return categoryId != null && List.of(1, 2, 3).contains(categoryId);
        }), "모든 결과의 categoryId는 1,2,3 중 하나여야 합니다.");
    }

    @Test
    @Order(7)
    void priceRangeFilterShouldReturnOnlyInRange() {
        SearchPrice price = new SearchPrice(5000, 15000);
        SearchRequest request = new SearchRequest(null, 10, price, null);
        List<SearchHitResult> results = vectorSearchService.search(request);

        System.out.println("[PRICE_FILTER] min=5000, max=15000");
        results.forEach(hit -> System.out.printf(
                "id=%s, name=%s, categoryId=%s, price=%s%n",
                hit.id(),
                hit.source().get("product_name"),
                hit.source().get("categoryId"),
                hit.source().get("price")
        ));

        Assertions.assertFalse(results.isEmpty(), "가격 범위 필터 결과는 비어있으면 안 됩니다.");
        Assertions.assertTrue(results.stream().allMatch(hit -> {
            Integer priceValue = asInteger(hit.source(), "price");
            return priceValue != null && priceValue >= 5000 && priceValue <= 15000;
        }), "모든 결과의 price는 5000~15000 범위여야 합니다.");
    }

    @Test
    @Order(8)
    void keywordCategoryAndPriceFilterShouldReturnMatchingResults() {
        SearchRequest request = new SearchRequest(
                "건강한 간식",
                10,
                new SearchPrice(5000, 30000),
                List.of(1,2,3)
        );
        List<SearchHitResult> results = vectorSearchService.search(request);

        System.out.println("[COMBINED_FILTER] query=건강한 간식, category=1, min=5000, max=30000");
        results.forEach(hit -> System.out.printf(
                "id=%s, score=%s, name=%s, categoryId=%s, price=%s%n",
                hit.id(),
                hit.score(),
                hit.source().get("product_name"),
                hit.source().get("categoryId"),
                hit.source().get("price")
        ));

        Assertions.assertFalse(results.isEmpty(), "복합 조건 결과는 비어있으면 안 됩니다.");
        Assertions.assertTrue(results.stream().allMatch(hit -> {
            Integer categoryId = asInteger(hit.source(), "categoryId");
            Integer priceValue = asInteger(hit.source(), "price");
            return categoryId != null
                    && categoryId == 1
                    && priceValue != null
                    && priceValue >= 5000
                    && priceValue <= 30000;
        }), "모든 결과가 카테고리/가격 조건을 충족해야 합니다.");
    }

    private void assertSemanticSearchContainsCategories(String query, int size, String... expectedCategoryKeywords) {
        // 검색 결과 출력 및 기대 카테고리 포함 여부 검증
        List<SearchHitResult> results = vectorSearchService.search(query, size);

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
        // 카테고리 문자열에 키워드가 포함되는지 체크
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

    private Integer asInteger(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

}
