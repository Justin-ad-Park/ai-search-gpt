package com.example.aisearch.service;

import com.example.aisearch.model.FoodProduct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class FoodDataLoader {

    private final ObjectMapper objectMapper;

    public FoodDataLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<FoodProduct> loadAll() {
        // classpath에 있는 JSON 샘플 데이터를 읽어 온다
        ClassPathResource resource = new ClassPathResource("data/food-products.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("샘플 데이터 로딩 실패", e);
        }
    }
}
