package com.example.aisearch.service.indexing;

import com.example.aisearch.service.indexing.orchestration.IndexRolloutResult;
import com.example.aisearch.service.indexing.orchestration.IndexRolloutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("indexing")
@ConditionalOnProperty(prefix = "ai-search", name = "run-index", havingValue = "true")
public class IndexingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexingRunner.class);

    private final IndexRolloutService indexRolloutService;
    private final ConfigurableApplicationContext context;

    public IndexingRunner(IndexRolloutService indexRolloutService,
                          ConfigurableApplicationContext context) {
        this.indexRolloutService = indexRolloutService;
        this.context = context;
    }

    @Override
    public void run(String... args) {
        IndexRolloutResult result = indexRolloutService.rollOutFromSourceData();
        log.info("Index rollout complete. oldIndex={}, newIndex={}, indexedCount={}",
                result.oldIndex(), result.newIndex(), result.indexedCount());

        // 배치 작업 완료 후 애플리케이션 종료
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }
}
