package com.example.aisearch;

import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Map;

public class IndexingApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AiSearchGptApplication.class)
                .properties(Map.of("ai-search.bootstrap-index", "true"))
                .run(args);
    }
}
