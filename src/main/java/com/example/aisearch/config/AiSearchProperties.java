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
    // 로컬 개발용: ES port-forward 자동 수행 여부
    private boolean autoPortForward;
    // k8s 네임스페이스
    private String k8sNamespace;
    // k8s ES HTTP 서비스 이름 (비어있으면 자동 탐지)
    private String k8sServiceName;
    // k8s Secret 이름 (elastic 비밀번호)
    private String k8sSecretName;
    // 로컬 포트
    private int k8sLocalPort;
    // 원격 포트
    private int k8sRemotePort;

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

    public boolean isAutoPortForward() {
        return autoPortForward;
    }

    public void setAutoPortForward(boolean autoPortForward) {
        this.autoPortForward = autoPortForward;
    }

    public String getK8sNamespace() {
        return k8sNamespace;
    }

    public void setK8sNamespace(String k8sNamespace) {
        this.k8sNamespace = k8sNamespace;
    }

    public String getK8sServiceName() {
        return k8sServiceName;
    }

    public void setK8sServiceName(String k8sServiceName) {
        this.k8sServiceName = k8sServiceName;
    }

    public String getK8sSecretName() {
        return k8sSecretName;
    }

    public void setK8sSecretName(String k8sSecretName) {
        this.k8sSecretName = k8sSecretName;
    }

    public int getK8sLocalPort() {
        return k8sLocalPort;
    }

    public void setK8sLocalPort(int k8sLocalPort) {
        this.k8sLocalPort = k8sLocalPort;
    }

    public int getK8sRemotePort() {
        return k8sRemotePort;
    }

    public void setK8sRemotePort(int k8sRemotePort) {
        this.k8sRemotePort = k8sRemotePort;
    }
}
