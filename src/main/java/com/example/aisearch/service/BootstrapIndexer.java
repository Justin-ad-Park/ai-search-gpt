package com.example.aisearch.service;

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
@ConditionalOnProperty(prefix = "ai-search", name = "bootstrap-index", havingValue = "true")
public class BootstrapIndexer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapIndexer.class);

    private final IndexManagementService indexManagementService;
    private final ProductIndexingService productIndexingService;
    private final ConfigurableApplicationContext context;

    public BootstrapIndexer(IndexManagementService indexManagementService,
                            ProductIndexingService productIndexingService,
                            ConfigurableApplicationContext context) {
        this.indexManagementService = indexManagementService;
        this.productIndexingService = productIndexingService;
        this.context = context;
    }

    @Override
    public void run(String... args) {
        // 애플리케이션 시작 직후 인덱스를 재생성하고 샘플 데이터를 색인
        indexManagementService.recreateIndex();
        long count = productIndexingService.reindexSampleData();
        log.info("Indexed {} documents into Elasticsearch", count);

        // 배치 작업 완료 후 애플리케이션 종료
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }
}
