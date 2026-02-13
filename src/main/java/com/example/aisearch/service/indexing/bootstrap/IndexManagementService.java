package com.example.aisearch.service.indexing.bootstrap;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import com.example.aisearch.service.synonym.SynonymReloadService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class IndexManagementService {
    private static final DateTimeFormatter VERSION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ElasticsearchClient esClient;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final IndexSchemaBuilder indexSchemaBuilder;
    private final SynonymReloadService synonymReloadService;

    public IndexManagementService(
            ElasticsearchClient esClient,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            IndexSchemaBuilder indexSchemaBuilder,
            SynonymReloadService synonymReloadService
    ) {
        this.esClient = esClient;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.indexSchemaBuilder = indexSchemaBuilder;
        this.synonymReloadService = synonymReloadService;
    }

    public void recreateIndex() {
        String indexName = properties.indexName();
        try {
            // 기존 인덱스가 있으면 삭제
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                esClient.indices().delete(d -> d.index(indexName));
            }

            synonymReloadService.ensureProductionSynonymsSet();

            // 임베딩 차원을 반영한 매핑 생성 (dims는 모델 차원과 반드시 일치해야 함)
            String mapping = indexSchemaBuilder.buildMapping(embeddingService.dimensions(), properties.synonymsSet());
            esClient.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mapping))
            );
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 생성 실패", e);
        }
    }

    public String createVersionedIndex() {
        String newIndex = nextVersionedIndexName(properties.indexName());
        createIndex(newIndex);
        return newIndex;
    }

    public void createIndex(String indexName) {
        try {
            synonymReloadService.ensureProductionSynonymsSet();
            String mapping = indexSchemaBuilder.buildMapping(embeddingService.dimensions(), properties.synonymsSet());
            esClient.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mapping))
            );
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 생성 실패. index=" + indexName, e);
        }
    }

    public String findCurrentAliasedIndex() {
        String alias = resolveReadAlias();
        try {
            boolean existsAlias = esClient.indices().existsAlias(e -> e.name(alias)).value();
            if (!existsAlias) {
                return null;
            }
            Map<String, ?> aliasMap = esClient.indices().getAlias(g -> g.name(alias)).result();
            return aliasMap.keySet().stream().findFirst().orElse(null);
        } catch (IOException e) {
            throw new IllegalStateException("alias 조회 실패. alias=" + alias, e);
        }
    }

    public void swapReadAlias(String oldIndex, String newIndex) {
        String alias = resolveReadAlias();
        try {
            boolean aliasNameAsIndexExists = esClient.indices().exists(e -> e.index(alias)).value();
            esClient.indices().updateAliases(u -> {
                // 마이그레이션 단계: alias 이름과 동일한 물리 인덱스가 있으면 먼저 제거해야 alias를 생성할 수 있다.
                if (aliasNameAsIndexExists && (oldIndex == null || oldIndex.isBlank())) {
                    u.actions(a -> a.removeIndex(r -> r.index(alias)));
                }

                if (oldIndex != null && !oldIndex.isBlank() && !oldIndex.equals(newIndex)) {
                    u.actions(a -> a.remove(r -> r.alias(alias).index(oldIndex)));
                }
                u.actions(a -> a.add(ad -> ad.alias(alias).index(newIndex)));
                return u;
            });
        } catch (IOException e) {
            throw new IllegalStateException("alias 전환 실패. alias=" + alias
                    + ", oldIndex=" + oldIndex + ", newIndex=" + newIndex, e);
        }
    }

    public void deleteIndexIfExists(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            return;
        }
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                esClient.indices().delete(d -> d.index(indexName));
            }
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 삭제 실패. index=" + indexName, e);
        }
    }

    private String nextVersionedIndexName(String baseIndex) {
        String version = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(VERSION_FORMATTER);
        return baseIndex + "-v" + version;
    }

    private String resolveReadAlias() {
        String readAlias = properties.readAlias();
        if (readAlias == null || readAlias.isBlank()) {
            return properties.indexName();
        }
        return readAlias;
    }
}
