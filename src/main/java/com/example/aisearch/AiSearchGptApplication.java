package com.example.aisearch;

import com.example.aisearch.config.AiSearchK8sProperties;
import com.example.aisearch.config.AiSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@EnableConfigurationProperties({AiSearchProperties.class, AiSearchK8sProperties.class})
public class AiSearchGptApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSearchGptApplication.class, args);
    }
}
