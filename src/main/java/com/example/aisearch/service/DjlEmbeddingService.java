package com.example.aisearch.service;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import com.example.aisearch.config.AiSearchProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Djl Embedding 서비스
 *  - 모델 설명은 docs/01.embedding-model.md 참고
 */
@Service
public class DjlEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DjlEmbeddingService.class);

    private final AiSearchProperties properties;
    private final ResourceLoader resourceLoader;
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;
    private int dimensions;

    public DjlEmbeddingService(AiSearchProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws ModelNotFoundException, MalformedModelException, IOException {
        // DJL 모델 로딩을 위한 조건 설정
        Criteria.Builder<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optProgress(new ProgressBar());

        String modelPath = properties.embeddingModelPath();
        if (modelPath != null && !modelPath.isBlank() && !"__NONE__".equalsIgnoreCase(modelPath.trim())) {
            // classpath: 경로를 실제 파일 경로로 변환
            Resource resource = resourceLoader.getResource(modelPath);
            var resolvedPath = resource.getFile().toPath();
            log.info("[EMBED_MODEL] using model path: {} -> {}", modelPath, resolvedPath);
            criteria.optModelPath(resolvedPath);
            // 로컬 모델은 translatorFactory를 명시해 줘야 안정적으로 로딩됨
            criteria.optTranslatorFactory(new TextEmbeddingTranslatorFactory());
        } else {
            // 기본값: DJL 지원 URL
            String modelUrl = properties.embeddingModelUrl();
            log.info("[EMBED_MODEL] using model url: {}", modelUrl);
            criteria.optModelUrls(modelUrl);
        }

        Criteria<String, float[]> buildCriteria = criteria.build();

        // 모델/예측기 로딩
        model = buildCriteria.loadModel();
        predictor = model.newPredictor();

        // 차원 수를 구하기 위해 1회 추론 (모델이 몇 차원 벡터를 만드는지 확인)
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
        // 각 요소를 벡터 길이(norm)로 나눠 단위 벡터로 만든다
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
