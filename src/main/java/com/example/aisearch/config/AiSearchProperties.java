package com.example.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-search")
public class AiSearchProperties {

    private String elasticsearchUrl;
    private String username;
    private String password;
    private String indexName;
    private String embeddingModelUrl;
    private double minScoreThreshold;
    private int numCandidatesMultiplier;
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
