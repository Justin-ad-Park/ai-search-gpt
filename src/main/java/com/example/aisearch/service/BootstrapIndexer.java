package com.example.aisearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-search", name = "bootstrap-index", havingValue = "true")
public class BootstrapIndexer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapIndexer.class);

    private final VectorIndexService vectorIndexService;

    public BootstrapIndexer(VectorIndexService vectorIndexService) {
        this.vectorIndexService = vectorIndexService;
    }

    @Override
    public void run(String... args) {
        vectorIndexService.recreateIndex();
        long count = vectorIndexService.reindexSampleData();
        log.info("Indexed {} documents into Elasticsearch", count);
    }
}
