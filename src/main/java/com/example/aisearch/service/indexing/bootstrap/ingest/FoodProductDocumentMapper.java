package com.example.aisearch.service.indexing.bootstrap.ingest;

import com.example.aisearch.model.FoodProduct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FoodProductDocumentMapper {

    public IndexDocument toIndexDocument(FoodProduct food, List<Float> embedding) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", food.getId());
        doc.put("product_name", food.getProductName());
        doc.put("category", food.getCategory());
        doc.put("categoryId", food.getCategoryId());
        doc.put("description", food.getDescription());
        doc.put("price", food.getPrice());
        doc.put("product_vector", embedding);
        return new IndexDocument(food.getId(), doc);
    }
}
