package com.example.aisearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.FoodProduct;
import com.example.aisearch.model.SearchHitResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorIndexService {

    private static final double MIN_SCORE_THRESHOLD = 0.74d;

    private final ElasticsearchClient client;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final FoodDataLoader foodDataLoader;

    public VectorIndexService(
            ElasticsearchClient client,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            FoodDataLoader foodDataLoader
    ) {
        this.client = client;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.foodDataLoader = foodDataLoader;
    }

    public void recreateIndex() {
        String indexName = properties.getIndexName();
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                client.indices().delete(d -> d.index(indexName));
            }

            String mapping = """
                    {
                      \"settings\": {
                        \"number_of_shards\": 1,
                        \"number_of_replicas\": 0
                      },
                      \"mappings\": {
                        \"properties\": {
                          \"id\": {\"type\": \"keyword\"},
                          \"product_name\": {\"type\": \"text\"},
                          \"category\": {\"type\": \"keyword\"},
                          \"description\": {\"type\": \"text\"},
                          \"product_vector\": {
                            \"type\": \"dense_vector\",
                            \"dims\": %d,
                            \"index\": true,
                            \"similarity\": \"cosine\"
                          }
                        }
                      }
                    }
                    """.formatted(embeddingService.dimensions());

            client.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mapping))
            );
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 생성 실패", e);
        }
    }

    public long reindexSampleData() {
        List<FoodProduct> foods = foodDataLoader.loadAll();
        if (foods.isEmpty()) {
            return 0;
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder().index(properties.getIndexName());

        for (FoodProduct food : foods) {
            float[] embedding = embeddingService.embed(food.toEmbeddingText());
            List<Float> vector = toFloatList(embedding);

            Map<String, Object> doc = new HashMap<>();
            doc.put("id", food.getId());
            doc.put("product_name", food.getProductName());
            doc.put("category", food.getCategory());
            doc.put("description", food.getDescription());
            doc.put("product_vector", vector);

            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .id(food.getId())
                            .document(doc)
                    )
            );
        }

        try {
            var response = client.bulk(bulkBuilder.refresh(Refresh.WaitFor).build());
            if (response.errors()) {
                throw new IllegalStateException("Bulk 인덱싱 중 일부 실패");
            }
            return foods.size();
        } catch (IOException e) {
            throw new IllegalStateException("Bulk 인덱싱 실패", e);
        }
    }

    public List<SearchHitResult> search(String query, int size) {
        float[] embedding = embeddingService.embed(query);
        List<Float> queryVector = toFloatList(embedding);

        try {
            SearchResponse<Map> response = client.search(s -> s
                            .index(properties.getIndexName())
                            .knn(knn -> knn
                                    .field("product_vector")
                                    .queryVector(queryVector)
                                    .k((long) size)
                                    .numCandidates((long) Math.max(size * 5, 50))
                            )
                            .size(size),
                    Map.class
            );

            List<SearchHitResult> results = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                String id = hit.id();
                Double score = hit.score();
                Map<String, Object> source = hit.source();
                if (score != null && score >= MIN_SCORE_THRESHOLD) {
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
