package com.example.aisearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.SearchHitResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VectorSearchService {

    private final ElasticsearchClient client;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;

    public VectorSearchService(
            ElasticsearchClient client,
            AiSearchProperties properties,
            EmbeddingService embeddingService
    ) {
        this.client = client;
        this.properties = properties;
        this.embeddingService = embeddingService;
    }

    public List<SearchHitResult> search(String query, int size) {
        float[] embedding = embeddingService.embed(query);
        List<Float> queryVector = toFloatList(embedding);

        try {
            long numCandidates = Math.max(
                    (long) size * properties.getNumCandidatesMultiplier(),
                    properties.getNumCandidatesMin()
            );
            SearchResponse<Map> response = client.search(s -> s
                            .index(properties.getIndexName())
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
                if (score != null && score >= properties.getMinScoreThreshold()) {
                    results.add(new SearchHitResult(id, score, source));
                }
            });
            return results;
        } catch (IOException e) {
            throw new IllegalStateException("벡터 검색 실패", e);
        }
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }
}
