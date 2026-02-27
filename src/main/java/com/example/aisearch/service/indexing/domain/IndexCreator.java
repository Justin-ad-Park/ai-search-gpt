package com.example.aisearch.service.indexing.domain;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.service.embedding.EmbeddingService;
import com.example.aisearch.service.indexing.bootstrap.schema.IndexSchemaBuilder;
import com.example.aisearch.service.indexing.domain.exception.IndexCreationException;
import com.example.aisearch.service.synonym.SynonymReloadService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;

/**
 * 신규 인덱스를 생성하는 도메인 서비스.
 *
 * 책임:
 * - 버전드 인덱스명 생성
 * - 동의어 세트 준비(프로덕션 기준)
 * - 임베딩 차원/동의어 설정을 반영한 매핑 생성
 * - Elasticsearch 인덱스 생성 호출
 */
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

    /**
     * 기본 인덱스명(properties.indexName)을 기준으로 버전드 인덱스를 생성한다.
     *
     * @return 생성된 신규 인덱스명
     */
    public String createVersionedIndex() {
        String newIndex = versionedIndexNameGenerator.generate(properties.indexName());
        createIndex(newIndex);
        return newIndex;
    }

    /**
     * 지정한 이름으로 인덱스를 생성한다.
     *
     * 동의어 세트 준비 후, 임베딩 차원을 반영한 매핑으로 인덱스를 생성한다.
     *
     * @param indexName 생성할 물리 인덱스명
     * @throws IndexCreationException 인덱스 생성 실패 시
     */
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
