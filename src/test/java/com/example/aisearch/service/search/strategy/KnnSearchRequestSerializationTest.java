package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.example.aisearch.model.search.SearchSortOption;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.stream.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KnnSearchRequestSerializationTest {

    private static final String SCRIPT_COMMON = """
            double vectorScore = (cosineSimilarity(params.query_vector, 'product_vector') + 1.0) / 2.0;
            double lexicalScore = Math.min(_score, 5.0) / 5.0;
            double base = 0.9 * vectorScore + 0.1 * lexicalScore;
            if (base < params.min_score_threshold) return 0.0;
            double categoryBoost = 0.0;
            """;

    private static final String CATEGORY_BOOST_BLOCK = """
            if (doc['categoryId'].size() != 0) {
              String categoryKey = String.valueOf(doc['categoryId'].value);
              def rawBoost = params.category_boost_by_id.get(categoryKey);
              if (rawBoost != null) {
                categoryBoost = ((Number) rawBoost).doubleValue();
              }
            }
            """;

    private static final String SCRIPT_RETURN_BLOCK = """
            double finalScore = base * (1.0 + params.beta * categoryBoost);
            return finalScore;
            """;

    private static final String BASE_SCRIPT = SCRIPT_COMMON + SCRIPT_RETURN_BLOCK;
    private static final String CATEGORY_BOOST_SCRIPT = SCRIPT_COMMON + CATEGORY_BOOST_BLOCK + SCRIPT_RETURN_BLOCK;

    @Test
    void shouldPrintSerializedSearchRequestJsonForTwoRepresentativeCases() throws Exception {
        SearchFilterQueryBuilder filterQueryBuilder = new SearchFilterQueryBuilder();

        com.example.aisearch.model.search.SearchRequest appleRequest =
                new com.example.aisearch.model.search.SearchRequest(
                        "사과",
                        null,
                        null,
                        SearchSortOption.CATEGORY_BOOSTING_DESC
                );
        CategoryBoostingResult boostDecision = CategoryBoostingResult.withBoost(Map.of("4", 0.2));
        Query appleBaseQuery = buildHybridBaseQuery(appleRequest, filterQueryBuilder.buildFilterQuery(appleRequest));
        SearchRequest appleEsRequest = SearchRequest.of(s -> s
                .index("food-products-read")
                .query(q -> q.scriptScore(ss -> ss
                        .query(appleBaseQuery)
                        .script(sc -> sc.inline(i -> {
                            i.lang("painless")
                                    .source(selectScriptSource(boostDecision))
                                    .params("query_vector", JsonData.of(List.of(0.11f, 0.22f, 0.33f)))
                                    .params("min_score_threshold", JsonData.of(0.74))
                                    .params("beta", JsonData.of(1.0));
                            if (boostDecision.applyCategoryBoost()) {
                                i.params("category_boost_by_id", JsonData.of(boostDecision.categoryBoostById()));
                            }
                            return i;
                        }))
                ))
                .sort(boostDecision.sortOptions())
                .trackScores(true)
                .from(0)
                .size(20)
                .minScore(0.74)
        );

        com.example.aisearch.model.search.SearchRequest filterOnlyRequest =
                new com.example.aisearch.model.search.SearchRequest(
                        null,
                        null,
                        List.of(4),
                        SearchSortOption.RELEVANCE_DESC
                );
        Query filterOnlyRootQuery = filterQueryBuilder.buildRootQuery(filterOnlyRequest);
        SearchRequest filterOnlyEsRequest = SearchRequest.of(s -> s
                .index("food-products-read")
                .query(filterOnlyRootQuery)
                .sort(filterOnlyRequest.sortOption().toSortOptions())
                .trackScores(true)
                .from(0)
                .size(20)
        );

        String appleJson = toPrettyJson(appleEsRequest);
        String filterOnlyJson = toPrettyJson(filterOnlyEsRequest);

        System.out.println("=== CASE A: apple + CATEGORY_BOOSTING_DESC ===");
        System.out.println(appleJson);
        System.out.println("=== CASE B: no query + categoryId filter + RELEVANCE_DESC ===");
        System.out.println(filterOnlyJson);

        assertTrue(appleJson.contains("\"script_score\""));
        assertTrue(appleJson.contains("\"category_boost_by_id\""));
        assertTrue(appleJson.contains("\"min_score_threshold\""));
        assertTrue(appleJson.contains("\"beta\""));
        assertTrue(!appleJson.contains("Math.min(1.0"));
        assertTrue(filterOnlyJson.contains("\"terms\""));
        assertTrue(!filterOnlyJson.contains("\"script_score\""));
    }

    private static Query buildHybridBaseQuery(
            com.example.aisearch.model.search.SearchRequest request,
            Optional<Query> filterQuery
    ) {
        Query lexicalQuery = Query.of(q -> q.multiMatch(mm -> mm
                .query(request.query())
                .fields("product_name^2", "description")
        ));

        return Query.of(q -> q.bool(b -> {
            filterQuery.ifPresent(b::filter);
            b.should(lexicalQuery);
            b.minimumShouldMatch("0");
            return b;
        }));
    }

    private static String selectScriptSource(CategoryBoostingResult decision) {
        return decision.applyCategoryBoost() ? CATEGORY_BOOST_SCRIPT : BASE_SCRIPT;
    }

    private static String toPrettyJson(SearchRequest request) throws Exception {
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        StringWriter writer = new StringWriter();
        JsonGenerator generator = mapper.jsonProvider().createGenerator(writer);
        request.serialize(generator, mapper);
        generator.close();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(objectMapper.readTree(writer.toString()));
    }
}
