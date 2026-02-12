package com.example.aisearch;

import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.controller.SearchController;
import com.example.aisearch.controller.dto.ReloadSynonymsRequestDto;
import com.example.aisearch.controller.dto.ReloadSynonymsResponseDto;
import com.example.aisearch.service.search.VectorSearchService;
import com.example.aisearch.service.synonym.SynonymEsGateway;
import com.example.aisearch.service.synonym.SynonymReloadMode;
import com.example.aisearch.service.synonym.SynonymReloadRequest;
import com.example.aisearch.service.synonym.SynonymReloadResult;
import com.example.aisearch.service.synonym.SynonymReloadService;
import com.example.aisearch.service.synonym.SynonymRuleSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynonymsTest {

    @Mock
    private SynonymRuleSource synonymRuleSource;
    @Mock
    private SynonymEsGateway synonymEsGateway;
    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private SynonymReloadService synonymReloadService;

    @Test
    void reloadSynonyms_regressionMode_appliesManduTteokgukRules() {
        SynonymReloadService service = new SynonymReloadService(testProperties(), synonymRuleSource, synonymEsGateway);
        List<String> rules = List.of("만두, 떡국");
        when(synonymRuleSource.loadRules(SynonymReloadMode.REGRESSION)).thenReturn(rules);

        SynonymReloadResult result = service.reload(new SynonymReloadRequest(SynonymReloadMode.REGRESSION, null, null));

        verify(synonymEsGateway).putSynonyms(eq("food-synonyms"), eq(rules));
        verify(synonymEsGateway).reloadSearchAnalyzers(eq("food-products-v1-test"));
        assertEquals("REGRESSION", result.mode());
        assertEquals(1, result.ruleCount());
    }

    @Test
    void reloadSynonyms_productionMode_restoresDefaultRules() {
        SynonymReloadService service = new SynonymReloadService(testProperties(), synonymRuleSource, synonymEsGateway);
        List<String> rules = List.of("만두, 교자, 얇은피, 얄피");
        when(synonymRuleSource.loadRules(SynonymReloadMode.PRODUCTION)).thenReturn(rules);

        SynonymReloadResult result = service.reload(SynonymReloadRequest.defaultRequest());

        verify(synonymEsGateway).putSynonyms(eq("food-synonyms"), eq(rules));
        verify(synonymEsGateway).reloadSearchAnalyzers(eq("food-products-v1-test"));
        assertEquals("PRODUCTION", result.mode());
        assertEquals(1, result.ruleCount());
    }

    @Test
    void search_tteokguk_returnsSaengMandu_afterRegressionReload() {
        SynonymReloadService service = new SynonymReloadService(testProperties(), synonymRuleSource, synonymEsGateway);
        List<String> rules = List.of("만두, 떡국");
        when(synonymRuleSource.loadRules(SynonymReloadMode.REGRESSION)).thenReturn(rules);

        service.reload(new SynonymReloadRequest(SynonymReloadMode.REGRESSION, null, null));

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(synonymEsGateway).putSynonyms(eq("food-synonyms"), captor.capture());
        assertTrue(captor.getValue().contains("만두, 떡국"));
    }

    @Test
    void search_yalpi_or_mandu_returnsMandu_afterProductionReload() {
        SynonymReloadService service = new SynonymReloadService(testProperties(), synonymRuleSource, synonymEsGateway);
        List<String> rules = List.of("만두, 교자, 얇은피, 얄피");
        when(synonymRuleSource.loadRules(SynonymReloadMode.PRODUCTION)).thenReturn(rules);

        service.reload(SynonymReloadRequest.defaultRequest());

        verify(synonymEsGateway).putSynonyms(eq("food-synonyms"), eq(rules));
    }

    @Test
    void reloadSynonyms_returns400_whenModeInvalid() {
        SearchController controller = new SearchController(vectorSearchService, synonymReloadService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.reloadSynonyms(new ReloadSynonymsRequestDto("INVALID_MODE", null, null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("invalid reload mode"));
    }

    @Test
    void reloadSynonyms_returns500_whenElasticsearchCallFails() {
        SearchController controller = new SearchController(vectorSearchService, synonymReloadService);
        when(synonymReloadService.reload(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("failed to reload analyzers for index ai-search-products"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.reloadSynonyms(new ReloadSynonymsRequestDto("PRODUCTION", null, null))
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getReason().contains("failed to reload analyzers"));
    }

    private AiSearchProperties testProperties() {
        return new AiSearchProperties(
                "http://localhost:9200",
                "elastic",
                "password",
                "food-products-v1-test",
                "food-synonyms",
                "classpath:es/dictionary/synonyms_ko.txt",
                "classpath:es/dictionary/synonyms_kr_regression.txt",
                "djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
                "classpath:/model/multilingual-e5-small-ko-v2",
                0.74,
                5,
                50
        );
    }
}
