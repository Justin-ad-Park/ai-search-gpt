package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KnnSearchStrategy implements SearchStrategy {

    private final ElasticsearchClient client;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final SearchFilterQueryBuilder filterQueryBuilder;

    public KnnSearchStrategy(
            ElasticsearchClient client,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            SearchFilterQueryBuilder filterQueryBuilder
    ) {
        this.client = client;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.filterQueryBuilder = filterQueryBuilder;
    }

    @Override
    public SearchPageResult search(SearchRequest searchRequest, Pageable pageable) {
        try {
            if (searchRequest.hasQuery()) {
                return vectorScoreSearch(searchRequest, pageable);
            }
            return filterOnlySearch(searchRequest, pageable);
        } catch (IOException e) {
            throw new IllegalStateException("벡터 검색 실패", e);
        }
    }

    private SearchPageResult vectorScoreSearch(SearchRequest request, Pageable pageable) throws IOException {
        int size = pageable.getPageSize();
        int from = (int) pageable.getOffset();
        float[] embedding = embeddingService.embed(request.query());
        List<Float> queryVector = toFloatList(embedding);
        Query baseQuery = buildHybridBaseQuery(request, filterQueryBuilder.buildFilterQuery(request));

        SearchResponse<Map> response = client.search(s -> s
                        .index(getReadAlias())
                        .query(q -> q.scriptScore(ss -> ss
                                .query(baseQuery)
                                .script(sc -> sc.inline(i -> i
                                        .lang("painless")
                                        .source(
                                                "double vectorScore = (cosineSimilarity(params.query_vector, 'product_vector') + 1.0) / 2.0; " +
                                                "double lexicalScore = Math.min(_score, 5.0) / 5.0; " +
                                                "return Math.min(1.0, 0.9 * vectorScore + 0.1 * lexicalScore + 0.1);"
                                        )
                                        .params("query_vector", JsonData.of(queryVector))
                                ))
                        ))
                        .sort(request.sortOption().toSortOptions())
                        .trackScores(true)
                        .from(from)
                        .size(size)
                        .minScore(properties.minScoreThreshold()),
                Map.class
        );
        List<SearchHitResult> results = toResults(response, true);
        return SearchPageResult.of(pageable, extractTotalHits(response), results);
    }

    private SearchPageResult filterOnlySearch(SearchRequest request, Pageable pageable) throws IOException {
        int size = pageable.getPageSize();
        int from = (int) pageable.getOffset();
        Query rootQuery = filterQueryBuilder.buildRootQuery(request);
        SearchResponse<Map> response = client.search(s -> s
                        .index(getReadAlias())
                        .query(rootQuery)
                        .sort(request.sortOption().toSortOptions())
                        .trackScores(true)
                        .from(from)
                        .size(size),
                Map.class
        );
        List<SearchHitResult> results = toResults(response, false);
        return SearchPageResult.of(pageable, extractTotalHits(response), results);
    }

    private Query buildHybridBaseQuery(SearchRequest request, java.util.Optional<Query> filterQuery) {
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

    private List<SearchHitResult> toResults(SearchResponse<Map> response, boolean applyScoreThreshold) {
        List<SearchHitResult> results = new ArrayList<>();
        response.hits().hits().forEach(hit -> {
            String id = hit.id();
            Double score = hit.score();
            Map<String, Object> source = hit.source();
            if (!applyScoreThreshold || (score != null && score >= properties.minScoreThreshold())) {
                results.add(new SearchHitResult(id, score, stripVector(source)));
            }
        });
        return results;
    }

    private Map<String, Object> stripVector(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        Map<String, Object> filtered = new java.util.HashMap<>(source);
        filtered.remove("product_vector");
        return filtered;
    }

    private long extractTotalHits(SearchResponse<Map> response) {
        if (response.hits() == null || response.hits().total() == null) {
            return 0L;
        }
        return response.hits().total().value();
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }

    private String getReadAlias() {
        String readAlias = properties.readAlias();
        if (readAlias == null || readAlias.isBlank()) {
            throw new IllegalStateException("ai-search.read-alias 값이 비어 있습니다.");
        }
        return readAlias;
    }
}
