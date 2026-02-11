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
     * 역할 1) 일반 API 서버 모드
     * - 구동: ./gradlew bootRun
     *
     * 역할 2) 배치 인덱싱 모드 (프로파일: indexing)
     * - 구동:
     *   ./gradlew bootRun --args='--spring.profiles.active=indexing'
     * - 효과: application-indexing.yml 설정이 적용되어
     *         웹 서버 없이 인덱싱 작업만 실행됨
     */
    public static void main(String[] args) {
        SpringApplication.run(AiSearchGptApplication.class, args);
    }
}
