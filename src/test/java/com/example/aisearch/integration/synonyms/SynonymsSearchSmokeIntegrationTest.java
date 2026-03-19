package com.example.aisearch.integration.synonyms;

import com.example.aisearch.service.synonym.SynonymReloadMode;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynonymsSearchSmokeIntegrationTest extends SynonymRestApiIntegrationTestBase {

    @Test
    void production_reload_후_얄피_검색은_비어있지_않다() throws Exception {
        reloadSynonymsAndAssert(SynonymReloadMode.PRODUCTION);

        JsonNode searchJson = searchAndAssertOk("얄피", 20);
        long resultCount = printSearchResults("얄피", searchJson, "생만두");

        assertTrue(resultCount > 0,
                "production 동의어 리로드 후 얄피 검색 결과는 비어있지 않아야 합니다.");
    }

    @Test
    void regression_reload_후_딤섬_검색은_비어있지_않다() throws Exception {
        reloadSynonymsAndAssert(SynonymReloadMode.REGRESSION);

        JsonNode searchJson = searchAndAssertOk("딤섬", 20);
        long resultCount = printSearchResults("딤섬", searchJson, "만두");

        assertTrue(resultCount > 0,
                "regression 동의어 리로드 후 딤섬 검색 결과는 비어있지 않아야 합니다.");

        searchJson = searchAndAssertOk("얄피", 20);
         resultCount = printSearchResults("얄피", searchJson, "생만두");

        // 얄피 검색에서 "얄피" 키워드가 없는 생만두는 검색이 되지 않아야 한다.
        assertEquals(0, resultCount, "얄피 검색에서 \"얄피\" 키워드가 없는 생만두는 검색이 되지 않아야 한다.");

    }
}
