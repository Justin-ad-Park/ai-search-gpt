package com.example.aisearch.service.search.categoryboost.policy;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 카테고리 부스팅 beta 값을 런타임에 조정하기 위한 튜너.
 */
@Component
public class CategoryBoostBetaTuner {

    public static final double DEFAULT_BETA = 1.0;

    private final AtomicReference<Double> beta = new AtomicReference<>(DEFAULT_BETA);

    public double getBeta() {
        return beta.get();
    }

    public void setBeta(double value) {
        if (value < 0.0) {
            throw new IllegalArgumentException("beta must be >= 0");
        }
        beta.set(value);
    }

    public void reset() {
        beta.set(DEFAULT_BETA);
    }
}
