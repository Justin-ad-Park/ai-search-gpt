package com.example.aisearch.integration.synonyms;

import com.example.aisearch.service.synonym.SynonymReloadMode;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynonymReloadApiIntegrationTest extends SynonymRestApiIntegrationTestBase {

    @Test
    void production_reload_api는_updated_reloaded_mode를_반환한다() throws Exception {
        JsonNode response = reloadSynonymsAndAssert(SynonymReloadMode.PRODUCTION);

        assertTrue(response.path("updated").asBoolean(false));
        assertTrue(response.path("reloaded").asBoolean(false));
        assertEquals("PRODUCTION", response.path("mode").asText());
    }

    @Test
    void regression_reload_api는_updated_reloaded_mode를_반환한다() throws Exception {
        JsonNode response = reloadSynonymsAndAssert(SynonymReloadMode.REGRESSION);

        assertTrue(response.path("updated").asBoolean(false));
        assertTrue(response.path("reloaded").asBoolean(false));
        assertEquals("REGRESSION", response.path("mode").asText());
    }
}
