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
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optModelUrls(properties.getEmbeddingModelUrl())
                .optProgress(new ProgressBar())
                .build();

        model = criteria.loadModel();
        predictor = model.newPredictor();

        float[] probe = predictRaw("한글 식품 벡터 검색 테스트");
        dimensions = probe.length;
    }

    @Override
    public float[] embed(String text) {
        float[] raw = predictRaw(text);
        return l2Normalize(raw);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private float[] predictRaw(String text) {
        try {
            return predictor.predict(text);
        } catch (TranslateException e) {
            throw new IllegalStateException("임베딩 생성 실패", e);
        }
    }

    private static float[] l2Normalize(float[] vector) {
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
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }
}
