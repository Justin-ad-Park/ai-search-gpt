package com.example.aisearch.service.indexing.orchestration;

public record IndexRolloutResult(
        String oldIndex,
        String newIndex,
        long indexedCount
) {
}
