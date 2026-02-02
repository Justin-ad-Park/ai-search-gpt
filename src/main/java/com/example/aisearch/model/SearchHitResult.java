package com.example.aisearch.model;

import java.util.Map;

public record SearchHitResult(String id, Double score, Map<String, Object> source) {
}
