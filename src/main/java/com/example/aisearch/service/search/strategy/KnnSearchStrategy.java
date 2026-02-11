package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.service.embedding.model.EmbeddingService;
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

    public KnnSearchStrategy(
            ElasticsearchClient client,
            AiSearchProperties properties,
            EmbeddingService embeddingService
    ) {
        this.client = client;
        this.properties = properties;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<SearchHitResult> search(String query, int size) {
        float[] embedding = embeddingService.embed(query);
        List<Float> queryVector = toFloatList(embedding);

        try {
            long numCandidates = Math.max(
                    (long) size * properties.numCandidatesMultiplier(),
                    properties.numCandidatesMin()
            );

            SearchResponse<Map> response = client.search(s -> s
                            .index(properties.indexName())
                            .knn(knn -> knn
                                    .field("product_vector")
                                    .queryVector(queryVector)
                                    .k((long) size)
                                    .numCandidates(numCandidates)
                            )
                            .size(size),
                    Map.class
            );

            List<SearchHitResult> results = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                String id = hit.id();
                Double score = hit.score();
                Map<String, Object> source = hit.source();
                if (score != null && score >= properties.minScoreThreshold()) {
                    results.add(new SearchHitResult(id, score, stripVector(source)));
                }
            });
            return results;
        } catch (IOException e) {
            throw new IllegalStateException("벡터 검색 실패", e);
        }
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
