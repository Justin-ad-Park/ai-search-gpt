package com.example.aisearch;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.service.IndexManagementService;
import com.example.aisearch.service.ProductIndexingService;
import com.example.aisearch.service.VectorSearchService;
import com.example.aisearch.support.ElasticsearchDirectExecutionSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VectorSearchIntegrationTest {

    private static final ElasticsearchDirectExecutionSetup.SetupResult SETUP_RESULT;

    static {
        // 테스트 실행 전 Elasticsearch 접속 준비
        SETUP_RESULT = ElasticsearchDirectExecutionSetup.setup();
    }

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
        long indexed = productIndexingService.reindexSampleData();
        Assertions.assertTrue(indexed >= 100, "최소 100건 이상 인덱싱되어야 합니다.");
        System.out.println("[INDEXED] total=" + indexed);
    }

    @Test
    @Order(2)
    void semanticSearchShouldReturnRelevantProducts() {
        // 아이 간식 관련 쿼리 테스트
        String query = "어린이가 먹을 만한 전통 스낵";
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

    @AfterAll
    static void teardown() {
        // 테스트 종료 후 포트포워딩 정리
        ElasticsearchDirectExecutionSetup.cleanup(SETUP_RESULT);
    }
}
