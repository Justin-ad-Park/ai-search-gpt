package com.example.aisearch.service.indexing.bootstrap;

import com.example.aisearch.model.FoodProduct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FoodProductDocumentMapper {

    public IndexDocument toIndexDocument(FoodProduct food, float[] embedding) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", food.getId());
        doc.put("product_name", food.getProductName());
        doc.put("category", food.getCategory());
        doc.put("description", food.getDescription());
        doc.put("product_vector", toFloatList(embedding));
        return new IndexDocument(food.getId(), doc);
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new java.util.ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }
}
