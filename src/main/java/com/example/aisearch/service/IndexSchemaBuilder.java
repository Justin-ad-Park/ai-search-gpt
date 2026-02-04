package com.example.aisearch.service;

import org.springframework.stereotype.Component;

@Component
public class IndexSchemaBuilder {

    public String buildMapping(int vectorDimensions) {
        // vectorDimensions는 임베딩 벡터 차원수
        return """
                {
                  "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0
                  },
                  "mappings": {
                    "properties": {
                      "id": {"type": "keyword"},
                      "product_name": {"type": "text"},
                      "category": {"type": "keyword"},
                      "description": {"type": "text"},
                      "product_vector": {
                        "type": "dense_vector",
                        "dims": %d,
                        "index": true,
                        "similarity": "cosine"
                      }
                    }
                  }
                }
                """.formatted(vectorDimensions);
    }
}
