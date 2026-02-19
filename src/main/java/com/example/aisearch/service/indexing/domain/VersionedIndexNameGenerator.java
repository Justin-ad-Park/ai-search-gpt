package com.example.aisearch.service.indexing.domain;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 기준 인덱스명에 버전 suffix를 붙여 신규 인덱스명을 생성한다.
 *
 * 형식: {baseIndexName}-vyyyyMMddHHmmss
 * 예: food-products-v20260219143015
 *
 * 목적:
 * - 인덱스 롤아웃 시 충돌 없는 새 인덱스명 보장
 * - 시간 기반으로 생성 시점을 추적 가능
 */
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
