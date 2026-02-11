package com.example.aisearch.service.embedding.model;

public interface EmbeddingService {

    // 텍스트를 임베딩 벡터로 변환
    float[] embed(String text);

    // 임베딩 벡터 차원 수
    int dimensions();
}
