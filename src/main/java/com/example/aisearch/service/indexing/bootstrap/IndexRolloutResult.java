package com.example.aisearch.service.indexing.bootstrap;

public record IndexRolloutResult(
        String oldIndex,
        String newIndex,
        long indexedCount
) {
}
