package com.example.aisearch.service.search.categoryboost.policy;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 카테고리 부스팅 강도를 제어하는 beta 값을 보관/조정한다.
 *
 * <p>검색 점수는 다음 형태로 계산된다.
 * <pre>{@code
 * finalScore = baseScore * (1 + beta * categoryBoost)
 * }</pre>
 *
 * <p>여기서 beta는 카테고리 부스트가 최종 점수에 미치는 영향도를 조절하는 다이얼이다.
 *  - 이 값은 추후 BOS 검색 관리 메뉴에서 관리할 수 있도록 하면 검색 담당자가 부스팅 영향력 조정을 쉽게 할 수 있다.
 * <ul>
 *   <li>{@code beta = 0.0}: 카테고리 부스팅 비활성 (baseScore만 사용)</li>
 *   <li>{@code beta = 1.0}: 룰에 정의된 categoryBoost를 그대로 반영</li>
 *   <li>{@code beta > 1.0}: 카테고리 부스팅 영향 확대</li>
 *   <li>{@code beta < 1.0}: 카테고리 부스팅 영향 축소</li>
 * </ul>
 *
 * <p>운영/실험 중 영향도를 빠르게 조정할 수 있도록 런타임 변경(set/reset)을 지원한다.
 * 내부 상태는 멀티스레드 환경에서 안전하도록 AtomicReference로 관리한다.
 */
@Component
public class CategoryBoostBetaTuner {

    public static final double DEFAULT_BETA = 1.0;

    private final AtomicReference<Double> beta = new AtomicReference<>(DEFAULT_BETA);

    /**
     * 현재 적용 중인 beta 값을 반환한다.
     */
    public double getBeta() {
        return beta.get();
    }

    /**
     * beta 값을 런타임에 변경한다.
     *
     * @param value 0 이상 값만 허용한다.
     */
    public void setBeta(double value) {
        if (value < 0.0) {
            throw new IllegalArgumentException("beta must be >= 0");
        }
        beta.set(value);
    }

    /**
     * beta 값을 기본값({@link #DEFAULT_BETA})으로 복원한다.
     */
    public void reset() {
        beta.set(DEFAULT_BETA);
    }
}
