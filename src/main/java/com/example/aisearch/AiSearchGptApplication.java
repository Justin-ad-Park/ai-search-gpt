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

    /*
역할 1) 일반 API 서버 모드

    - 검색 웹 서버만 실행
        ./gradlew bootRun

    - 색인 + 검색 웹 서버:
        ./gradlew bootRun --args='--spring.profiles.active=indexing-web'

    - 색인만 실행하고 종료:
        ./gradlew bootRun --args='--spring.profiles.active=indexing'

     */
    public static void main(String[] args) {
        SpringApplication.run(AiSearchGptApplication.class, args);
    }
}
