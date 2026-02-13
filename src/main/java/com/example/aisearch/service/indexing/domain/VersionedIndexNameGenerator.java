package com.example.aisearch.service.indexing.domain;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class VersionedIndexNameGenerator {

    private static final DateTimeFormatter VERSION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public VersionedIndexNameGenerator() {
        this.clock = Clock.system(DEFAULT_ZONE);
    }

    String generate(String baseIndexName) {
        String version = LocalDateTime.now(clock).format(VERSION_FORMATTER);
        return baseIndexName + "-v" + version;
    }
}
