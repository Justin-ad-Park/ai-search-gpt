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
    private static final String SYNONYMS_SET_PLACEHOLDER = "__SYNONYMS_SET_ID__";

    private final ResourceLoader resourceLoader;

    public IndexSchemaBuilder(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String buildMapping(int vectorDimensions, String synonymsSetId) {
        // vectorDimensions는 임베딩 벡터 차원수
        String template = loadTemplate(MAPPING_TEMPLATE);
        return template
                .replace(DIMS_PLACEHOLDER, String.valueOf(vectorDimensions))
                .replace(SYNONYMS_SET_PLACEHOLDER, synonymsSetId);
    }

    private String loadTemplate(String mappingTemplate) {
        Resource resource = resourceLoader.getResource(mappingTemplate);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("인덱스 매핑 템플릿 로딩 실패", e);
        }
    }
}
