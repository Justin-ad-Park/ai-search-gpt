package com.example.aisearch.service;

public interface EmbeddingService {

    float[] embed(String text);

    int dimensions();
}
