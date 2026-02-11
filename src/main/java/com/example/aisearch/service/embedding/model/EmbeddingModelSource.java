package com.example.aisearch.service.embedding.model;

import java.nio.file.Path;

public record EmbeddingModelSource(Path modelPath, String modelUrl, boolean requiresTranslatorFactory) {
    public boolean isPathBased() {
        return modelPath != null;
    }
}
