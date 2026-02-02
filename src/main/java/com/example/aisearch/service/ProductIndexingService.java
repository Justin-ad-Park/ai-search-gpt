package com.example.aisearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.FoodProduct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductIndexingService {

    private final ElasticsearchClient client;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final FoodDataLoader foodDataLoader;

    public ProductIndexingService(
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

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new java.util.ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }
}
