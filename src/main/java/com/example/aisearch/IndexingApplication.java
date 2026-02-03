package com.example.aisearch;

import com.example.aisearch.support.ElasticsearchDirectExecutionSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

public class IndexingApplication {

    public static void main(String[] args) {
        ElasticsearchDirectExecutionSetup.SetupResult setupResult = ElasticsearchDirectExecutionSetup.setup();
        if (setupResult.portForwardProcess() != null) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> ElasticsearchDirectExecutionSetup.cleanup(setupResult))
            );
        }
        System.setProperty("ai-search.bootstrap-index", "true");
        SpringApplication application = new SpringApplication(AiSearchGptApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }
}
