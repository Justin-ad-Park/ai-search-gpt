package com.example.aisearch.service.embedding.model;

import java.util.List;

public interface EmbeddingService {

    // 텍스트를 임베딩 벡터로 변환
    List<Float> toEmbeddingVector(String text);

    // 임베딩 벡터 차원 수
    int dimensions();
}
