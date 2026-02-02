package com.example.aisearch;

import com.example.aisearch.config.AiSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiSearchProperties.class)
public class AiSearchGptApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSearchGptApplication.class, args);
    }
}
