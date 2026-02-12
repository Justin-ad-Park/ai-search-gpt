package com.example.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-search")
public record AiSearchProperties(
    /**
     * Elasticsearch 접속 정보
     */
    String elasticsearchUrl,
    String username,
    String password,
    /**
     * 색인명
     */
    String indexName,
    /**
     * 동의어 세트 ID
     */
    String synonymsSet,
    /**
     * 운영 동의어 파일 경로
     */
    String synonymsFilePath,
    /**
     * 회귀 테스트 동의어 파일 경로
     */
    String synonymsRegressionFilePath,
    /**
     * 임베딩 모델 위치 (DJL 지원 URL)
     */
    String embeddingModelUrl,
    /**
     * 임베딩 모델 로컬 경로 (classpath: 또는 파일 시스템 경로)
     */
    String embeddingModelPath,
    /**
     * 검색 결과 필터링 최소 점수
     */
    double minScoreThreshold,
    /**
     * 후보군 확장 배수
     */
    int numCandidatesMultiplier,
    /**
     * 후보군 최소 개수
     */
    int numCandidatesMin
) {
}
