package com.example.aisearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.config.AiSearchProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;

@Service
public class IndexManagementService {

    private final ElasticsearchClient client;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final IndexSchemaBuilder indexSchemaBuilder;

    public IndexManagementService(
            ElasticsearchClient client,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            IndexSchemaBuilder indexSchemaBuilder
    ) {
        this.client = client;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.indexSchemaBuilder = indexSchemaBuilder;
    }

    public void recreateIndex() {
        String indexName = properties.getIndexName();
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                client.indices().delete(d -> d.index(indexName));
            }

            String mapping = indexSchemaBuilder.buildMapping(embeddingService.dimensions());

            client.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mapping))
            );
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 생성 실패", e);
        }
    }
}
