package com.example.aisearch.service.synonym;

import com.example.aisearch.config.AiSearchProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class FileSynonymRuleSource implements SynonymRuleSource {

    private final ResourceLoader resourceLoader;
    private final AiSearchProperties properties;

    public FileSynonymRuleSource(ResourceLoader resourceLoader, AiSearchProperties properties) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    @Override
    public List<String> loadRules(SynonymReloadMode mode) {
        String filePath = resolveFilePath(mode);
        String content = loadContent(filePath);
        return Arrays.stream(content.split("\\R"))
                .map(this::normalizeRuleLine)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String resolveFilePath(SynonymReloadMode mode) {
        String filePath = mode == SynonymReloadMode.REGRESSION
                ? properties.synonymsRegressionFilePath()
                : properties.synonymsFilePath();
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("동의어 파일 경로가 설정되지 않았습니다. mode=" + mode);
        }
        return filePath;
    }

    private String loadContent(String filePath) {
        Resource resource = resourceLoader.getResource(filePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("동의어 파일 로딩 실패: " + filePath, e);
        }
    }

    private String normalizeRuleLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank() || trimmed.startsWith("#")) {
            return "";
        }
        int commentIndex = trimmed.indexOf('#');
        return commentIndex >= 0 ? trimmed.substring(0, commentIndex).trim() : trimmed;
    }
}
