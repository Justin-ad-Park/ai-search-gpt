package com.example.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-search")
public class AiSearchProperties {

    // Elasticsearch 접속 정보
    private String elasticsearchUrl;
    private String username;
    private String password;
    // 색인명
    private String indexName;
    // 임베딩 모델 위치 (DJL 지원 URL)
    private String embeddingModelUrl;
    // 임베딩 모델 로컬 경로 (classpath: 또는 파일 시스템 경로)
    private String embeddingModelPath;
    // 검색 결과 필터링 최소 점수
    private double minScoreThreshold;
    // 후보군 확장 배수
    private int numCandidatesMultiplier;
    // 후보군 최소 개수
    private int numCandidatesMin;

    public String getElasticsearchUrl() {
        return elasticsearchUrl;
    }

    public void setElasticsearchUrl(String elasticsearchUrl) {
        this.elasticsearchUrl = elasticsearchUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getEmbeddingModelUrl() {
        return embeddingModelUrl;
    }

    public void setEmbeddingModelUrl(String embeddingModelUrl) {
        this.embeddingModelUrl = embeddingModelUrl;
    }

    public String getEmbeddingModelPath() {
        return embeddingModelPath;
    }

    public void setEmbeddingModelPath(String embeddingModelPath) {
        this.embeddingModelPath = embeddingModelPath;
    }

    public double getMinScoreThreshold() {
        return minScoreThreshold;
    }

    public void setMinScoreThreshold(double minScoreThreshold) {
        this.minScoreThreshold = minScoreThreshold;
    }

    public int getNumCandidatesMultiplier() {
        return numCandidatesMultiplier;
    }

    public void setNumCandidatesMultiplier(int numCandidatesMultiplier) {
        this.numCandidatesMultiplier = numCandidatesMultiplier;
    }

    public int getNumCandidatesMin() {
        return numCandidatesMin;
    }

    public void setNumCandidatesMin(int numCandidatesMin) {
        this.numCandidatesMin = numCandidatesMin;
    }
}
