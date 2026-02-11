package com.example.aisearch.service;

import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.FoodProduct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductIndexingService {

    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final FoodDataLoader foodDataLoader;
    private final FoodProductDocumentMapper documentMapper;
    private final BulkIndexingExecutor bulkIndexingExecutor;

    public ProductIndexingService(
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            FoodDataLoader foodDataLoader,
            FoodProductDocumentMapper documentMapper,
            BulkIndexingExecutor bulkIndexingExecutor
    ) {
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.foodDataLoader = foodDataLoader;
        this.documentMapper = documentMapper;
        this.bulkIndexingExecutor = bulkIndexingExecutor;
    }

    public long reindexSampleData() {
        // 샘플 데이터 로딩
        List<FoodProduct> foods = foodDataLoader.loadAll();
        if (foods.isEmpty()) {
            return 0;
        }

        List<IndexDocument> documents = foods.stream()
                .map(food -> documentMapper.toIndexDocument(
                        food,
                        embeddingService.embed(food.toEmbeddingText())
                ))
                .collect(Collectors.toList());

        // refresh=wait_for로 바로 검색 가능 상태로 만듦 (색인 직후 검색 테스트 용도)
        return bulkIndexingExecutor.bulkIndex(properties.indexName(), documents);
    }
}
