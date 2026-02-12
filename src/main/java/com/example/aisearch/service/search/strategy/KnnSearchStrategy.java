package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
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
    public List<SearchHitResult> search(SearchRequest searchRequest) {
        try {
            if (searchRequest.hasQuery()) {
                return knnSearch(searchRequest);
            }
            return filterOnlySearch(searchRequest);
        } catch (IOException e) {
            throw new IllegalStateException("벡터 검색 실패", e);
        }
    }

    private List<SearchHitResult> knnSearch(SearchRequest request) throws IOException {
        int size = request.size();
        float[] embedding = embeddingService.embed(request.query());
        List<Float> queryVector = toFloatList(embedding);
        long numCandidates = Math.max(
                (long) size * properties.numCandidatesMultiplier(),
                properties.numCandidatesMin()
        );
        Query filterQuery = filterQueryBuilder.buildFilterQuery(request);

        SearchResponse<Map> response = client.search(s -> {
                    s.index(properties.indexName());
                    s.size(size);
                    s.knn(knn -> {
                        knn.field("product_vector");
                        knn.queryVector(queryVector);
                        knn.k((long) size);
                        knn.numCandidates(numCandidates);
                        if (filterQuery != null) {
                            knn.filter(filterQuery);
                        }
                        return knn;
                    });
                    return s;
                },
                Map.class
        );
        return toResults(response, true);
    }

    private List<SearchHitResult> filterOnlySearch(SearchRequest request) throws IOException {
        int size = request.size();
        Query rootQuery = filterQueryBuilder.buildRootQuery(request);
        SearchResponse<Map> response = client.search(s -> s
                        .index(properties.indexName())
                        .query(rootQuery)
                        .size(size),
                Map.class
        );
        return toResults(response, false);
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

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }
}
