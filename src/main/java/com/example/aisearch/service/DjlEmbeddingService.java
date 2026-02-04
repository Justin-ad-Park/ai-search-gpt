package com.example.aisearch.service;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.example.aisearch.config.AiSearchProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DjlEmbeddingService implements EmbeddingService {

    private final AiSearchProperties properties;
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;
    private int dimensions;

    public DjlEmbeddingService(AiSearchProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws ModelNotFoundException, MalformedModelException, IOException {
        // DJL 모델 로딩을 위한 조건 설정
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optModelUrls(properties.getEmbeddingModelUrl())
                .optProgress(new ProgressBar())
                .build();

        // 모델/예측기 로딩
        model = criteria.loadModel();
        predictor = model.newPredictor();

        // 차원 수를 구하기 위해 1회 추론
        float[] probe = predictRaw("한글 식품 벡터 검색 테스트");
        dimensions = probe.length;
    }

    @Override
    public float[] embed(String text) {
        // 임베딩 생성 후 L2 정규화
        float[] raw = predictRaw(text);
        return l2Normalize(raw);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private float[] predictRaw(String text) {
        try {
            // DJL Predictor로 텍스트 임베딩 추론
            return predictor.predict(text);
        } catch (TranslateException e) {
            throw new IllegalStateException("임베딩 생성 실패", e);
        }
    }

    private static float[] l2Normalize(float[] vector) {
        // 코사인 유사도 계산에 적합하도록 L2 정규화
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

    @PreDestroy
    public void close() {
        // 리소스 정리
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }
}
