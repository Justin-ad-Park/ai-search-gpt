package com.example.aisearch.service.indexing.bootstrap;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class BulkIndexingExecutor {

    private final ElasticsearchClient client;

    public BulkIndexingExecutor(ElasticsearchClient client) {
        this.client = client;
    }

    public long bulkIndex(String indexName, List<IndexDocument> documents) {
        if (documents.isEmpty()) {
            return 0;
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder().index(indexName);
        for (IndexDocument doc : documents) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .id(doc.id())
                            .document(doc.document())
                    )
            );
        }

        try {
            var response = client.bulk(bulkBuilder.refresh(Refresh.WaitFor).build());
            if (response.errors()) {
                String detail = response.items().stream()
                        .filter(item -> item.error() != null)
                        .limit(3)
                        .map(item -> "id=" + item.id()
                                + ", type=" + item.error().type()
                                + ", reason=" + item.error().reason())
                        .reduce((a, b) -> a + " | " + b)
                        .orElse("원인 미확인");
                throw new IllegalStateException("Bulk 인덱싱 중 일부 실패: " + detail);
            }
            return documents.size();
        } catch (IOException e) {
            throw new IllegalStateException("Bulk 인덱싱 실패", e);
        }
    }
}
