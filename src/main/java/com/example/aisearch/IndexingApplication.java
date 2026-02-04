package com.example.aisearch;

import com.example.aisearch.support.ElasticsearchDirectExecutionSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

public class IndexingApplication {

    public static void main(String[] args) {
        // 로컬에서 Elasticsearch 접속을 자동으로 준비(포트포워딩 등)
        ElasticsearchDirectExecutionSetup.SetupResult setupResult = ElasticsearchDirectExecutionSetup.setup();
        if (setupResult.portForwardProcess() != null) {
            // 애플리케이션 종료 시 포트포워딩 정리
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> ElasticsearchDirectExecutionSetup.cleanup(setupResult))
            );
        }
        // 인덱싱 전용 모드 활성화
        System.setProperty("ai-search.bootstrap-index", "true");
        SpringApplication application = new SpringApplication(AiSearchGptApplication.class);
        // 웹 서버를 띄우지 않고 배치처럼 실행
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }
}
