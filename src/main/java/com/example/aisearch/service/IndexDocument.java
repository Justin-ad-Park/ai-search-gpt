package com.example.aisearch.service;

import java.util.Map;

public record IndexDocument(String id, Map<String, Object> document) {
}
