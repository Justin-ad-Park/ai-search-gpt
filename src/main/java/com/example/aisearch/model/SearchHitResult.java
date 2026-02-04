package com.example.aisearch.model;

import java.util.Map;

// 검색 결과 한 건을 담는 DTO
public record SearchHitResult(String id, Double score, Map<String, Object> source) {
}
