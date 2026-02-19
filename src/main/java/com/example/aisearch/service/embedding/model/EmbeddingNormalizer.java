package com.example.aisearch.service.embedding.model;

import org.springframework.stereotype.Component;

/**
 * 벡터의 크기값을 없애고, 방향값만 남기는 함수(메서드)
 * - 단어의 유사한 정도는 크기보다 방향이 더 적합하기 때문에 방향값만 사용함
 * - 노멀라이저 설명 :
 *  EmbeddingNormalizer.md
 */
@Component
public class EmbeddingNormalizer {

    public float[] l2Normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }
}
