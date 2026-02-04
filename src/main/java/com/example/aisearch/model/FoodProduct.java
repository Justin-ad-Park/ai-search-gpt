package com.example.aisearch.model;

public class FoodProduct {

    // 상품 ID
    private String id;
    // 상품명
    private String productName;
    // 카테고리
    private String category;
    // 설명
    private String description;

    public FoodProduct() {
    }

    public FoodProduct(String id, String productName, String category, String description) {
        this.id = id;
        this.productName = productName;
        this.category = category;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toEmbeddingText() {
        // 검색 품질을 위해 여러 필드를 합쳐 임베딩 텍스트로 사용
        return productName + " " + category + " " + description;
    }
}
