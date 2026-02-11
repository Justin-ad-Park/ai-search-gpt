package com.example.aisearch.service.indexing.bootstrap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class IndexSchemaBuilder {

    private static final String MAPPING_TEMPLATE = "classpath:es/index-mapping.json";
    private static final String DIMS_PLACEHOLDER = "__DIMS__";

    private final ResourceLoader resourceLoader;

    public IndexSchemaBuilder(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String buildMapping(int vectorDimensions) {
        // vectorDimensions는 임베딩 벡터 차원수
        String template = loadTemplate();
        return template.replace(DIMS_PLACEHOLDER, String.valueOf(vectorDimensions));
    }

    private String loadTemplate() {
        Resource resource = resourceLoader.getResource(MAPPING_TEMPLATE);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 매핑 템플릿 로딩 실패", e);
        }
    }
}
