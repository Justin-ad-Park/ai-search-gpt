package com.example.aisearch.service.indexing.bootstrap;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;

@Service
public class IndexManagementService {

    private final ElasticsearchClient esClient;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final IndexSchemaBuilder indexSchemaBuilder;

    public IndexManagementService(
            ElasticsearchClient esClient,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            IndexSchemaBuilder indexSchemaBuilder
    ) {
        this.esClient = esClient;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.indexSchemaBuilder = indexSchemaBuilder;
    }

    public void recreateIndex() {
        String indexName = properties.indexName();
        try {
            // 기존 인덱스가 있으면 삭제
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                esClient.indices().delete(d -> d.index(indexName));
            }

            // 임베딩 차원을 반영한 매핑 생성 (dims는 모델 차원과 반드시 일치해야 함)
            String mapping = indexSchemaBuilder.buildMapping(embeddingService.dimensions());

            // 새 인덱스 생성
            esClient.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mapping))
            );
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 생성 실패", e);
        }
    }
}
