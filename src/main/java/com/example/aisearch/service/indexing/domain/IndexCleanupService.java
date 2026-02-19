package com.example.aisearch.service.indexing.domain;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.service.indexing.domain.exception.IndexCleanupException;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 롤아웃 후 불필요해진 인덱스를 정리하는 도메인 서비스.
 */
@Service
public class IndexCleanupService {

    private final ElasticsearchClient esClient;

    public IndexCleanupService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 인덱스가 존재할 때만 삭제한다.
     *
     * @param indexName 삭제 대상 인덱스명(null/blank면 무시)
     * @throws IndexCleanupException 삭제 처리 중 I/O 실패 시
     */
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
            throw new IndexCleanupException("인덱스 삭제 실패. index=" + indexName, e);
        }
    }
}
