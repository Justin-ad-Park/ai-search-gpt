package com.example.aisearch.service.indexing.domain;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.aisearch.service.indexing.domain.exception.IndexCleanupException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class IndexCleanupService {

    private final ElasticsearchClient esClient;

    public IndexCleanupService(ElasticsearchClient esClient) {
        this.esClient = esClient;
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
            throw new IndexCleanupException("인덱스 삭제 실패. index=" + indexName, e);
        }
    }
}
