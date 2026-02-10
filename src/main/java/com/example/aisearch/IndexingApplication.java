package com.example.aisearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

public class IndexingApplication {

    public static void main(String[] args) {
        // 인덱싱 전용 모드 활성화
        System.setProperty("ai-search.bootstrap-index", "true");
        SpringApplication application = new SpringApplication(AiSearchGptApplication.class);
        // 웹 서버를 띄우지 않고 배치처럼 실행
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }
}
