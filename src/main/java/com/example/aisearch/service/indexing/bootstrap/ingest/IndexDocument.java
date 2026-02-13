package com.example.aisearch.service.indexing.bootstrap.ingest;

import java.util.Map;

public record IndexDocument(String id, Map<String, Object> document) {
}
