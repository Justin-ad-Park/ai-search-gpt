package com.example.aisearch.service.embedding.model;

import com.example.aisearch.config.AiSearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class EmbeddingModelSourceLoader {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelSourceLoader.class);
    private final ResourceLoader resourceLoader;
    private final AiSearchProperties properties;

    public EmbeddingModelSourceLoader(ResourceLoader resourceLoader, AiSearchProperties properties) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    public EmbeddingModelSource load() throws IOException {
        String modelPath = properties.embeddingModelPath();
        if (modelPath != null && !modelPath.isBlank() && !"__NONE__".equalsIgnoreCase(modelPath.trim())) {
            Resource resource = resourceLoader.getResource(modelPath);
            Path resolvedPath = resource.getFile().toPath();
            log.info("[EMBED_MODEL] using model path: {} -> {}", modelPath, resolvedPath);
            return new EmbeddingModelSource(resolvedPath, null, true);
        }

        String modelUrl = properties.embeddingModelUrl();
        log.info("[EMBED_MODEL] using model url: {}", modelUrl);
        return new EmbeddingModelSource(null, modelUrl, false);
    }
}
