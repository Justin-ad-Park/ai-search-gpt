package com.example.aisearch.service.indexing.domain;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import com.example.aisearch.service.indexing.bootstrap.schema.IndexSchemaBuilder;
import com.example.aisearch.service.indexing.domain.exception.IndexCreationException;
import com.example.aisearch.service.synonym.SynonymReloadService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;

@Service
public class IndexCreator {

    private final ElasticsearchClient esClient;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final IndexSchemaBuilder indexSchemaBuilder;
    private final SynonymReloadService synonymReloadService;
    private final VersionedIndexNameGenerator versionedIndexNameGenerator;

    public IndexCreator(
            ElasticsearchClient esClient,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            IndexSchemaBuilder indexSchemaBuilder,
            SynonymReloadService synonymReloadService,
            VersionedIndexNameGenerator versionedIndexNameGenerator
    ) {
        this.esClient = esClient;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.indexSchemaBuilder = indexSchemaBuilder;
        this.synonymReloadService = synonymReloadService;
        this.versionedIndexNameGenerator = versionedIndexNameGenerator;
    }

    public String createVersionedIndex() {
        String newIndex = versionedIndexNameGenerator.generate(properties.indexName());
        createIndex(newIndex);
        return newIndex;
    }

    public void createIndex(String indexName) {
        try {
            synonymReloadService.ensureProductionSynonymsSet();
            String mapping = indexSchemaBuilder.buildMapping(embeddingService.dimensions(), properties.synonymsSet());
            esClient.indices().create(c -> c.index(indexName).withJson(new StringReader(mapping)));
        } catch (IOException e) {
            throw new IndexCreationException("인덱스 생성 실패. index=" + indexName, e);
        }
    }
}
