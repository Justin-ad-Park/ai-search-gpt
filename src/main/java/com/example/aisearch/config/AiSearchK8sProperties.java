package com.example.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-search.k8s")
public record AiSearchK8sProperties(
    /**
     * 로컬 개발용: ES port-forward 자동 수행 여부
     */
    boolean autoPortForward,
    /**
     * k8s 네임스페이스
     */
    String namespace,
    /**
     * k8s ES HTTP 서비스 이름 (비어있으면 자동 탐지)
     */
    String serviceName,
    /**
     * k8s Secret 이름 (elastic 비밀번호)
     */
    String secretName,
    /**
     * 로컬 포트
     */
    int localPort,
    /**
     * 원격 포트
     */
    int remotePort
) {
}
