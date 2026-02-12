package com.example.aisearch.service.embedding;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.example.aisearch.service.embedding.model.EmbeddingModelSource;
import com.example.aisearch.service.embedding.model.EmbeddingModelSourceLoader;
import com.example.aisearch.service.embedding.model.EmbeddingNormalizer;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Djl Embedding 서비스
 *  - 모델 설명은 docs/01.embedding-model.md 참고
 */
@Service
public class DjlEmbeddingService implements EmbeddingService {

    private final EmbeddingModelSourceLoader modelSourceResolver;
    private final EmbeddingNormalizer embeddingNormalizer;
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;
    private int dimensions;

    public DjlEmbeddingService(
            EmbeddingModelSourceLoader modelSourceResolver,
            EmbeddingNormalizer embeddingNormalizer
    ) {
        this.modelSourceResolver = modelSourceResolver;
        this.embeddingNormalizer = embeddingNormalizer;
    }

    @PostConstruct
    public void init() throws ModelNotFoundException, MalformedModelException, IOException {
        // DJL 모델 로딩을 위한 조건 설정
        Criteria.Builder<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optProgress(new ProgressBar());

        EmbeddingModelSource modelSource = modelSourceResolver.load();
        if (modelSource.isPathBased()) {
            criteria.optModelPath(modelSource.modelPath());
            if (modelSource.requiresTranslatorFactory()) {
                criteria.optTranslatorFactory(new ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory());
            }
        } else {
            criteria.optModelUrls(modelSource.modelUrl());
        }

        Criteria<String, float[]> buildCriteria = criteria.build();

        // 모델/예측기 로딩
        model = buildCriteria.loadModel();
        predictor = model.newPredictor();

        // 차원 수를 구하기 위해 1회 추론 (모델이 몇 차원 벡터를 만드는지 확인)
        float[] probe = predictRaw("한글 식품 벡터 검색 테스트");
        dimensions = probe.length;

        System.out.println("\n\n##dimensions##: " + dimensions);
    }

    @Override
    public float[] embed(String text) {
        // 임베딩 생성 후 L2 정규화
        float[] raw = predictRaw(text);
        return embeddingNormalizer.l2Normalize(raw);
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
