package com.example.aisearch.model;

public class FoodProduct {

    private String id;
    private String productName;
    private String category;
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
        return productName + " " + category + " " + description;
    }
}
