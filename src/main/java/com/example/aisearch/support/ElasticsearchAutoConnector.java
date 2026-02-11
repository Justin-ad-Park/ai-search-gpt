package com.example.aisearch.support;

import com.example.aisearch.config.AiSearchK8sProperties;
import com.example.aisearch.config.AiSearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Elasticsearch 접속 정보를 자동으로 보정하는 오케스트레이터.
 *
 * <p>로컬 개발 환경에서 k8s 포트포워딩이 필요할 때:
 * - kubectl 사용 가능 여부 확인
 * - 서비스 이름 자동 탐지
 * - Secret에서 비밀번호 로딩
 * - localhost URL로 재조합
 *
 * <p>그 외 환경(원격/프로덕션 등)에서는 원래 URL을 그대로 사용한다.
 */
@Component
public class ElasticsearchAutoConnector {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchAutoConnector.class);

    private final PortForwardDecision portForwardDecision;
    private final K8sPortForwarder k8sPortForwarder;
    private final ElasticPasswordProvider passwordProvider;
    private final ElasticsearchUrlBuilder urlBuilder;

    /**
     * 포트포워딩 판단/실행/비밀번호 로딩/URL 조합 역할을 가진 컴포넌트를 주입한다.
     */
    public ElasticsearchAutoConnector(
            PortForwardDecision portForwardDecision,
            K8sPortForwarder k8sPortForwarder,
            ElasticPasswordProvider passwordProvider,
            ElasticsearchUrlBuilder urlBuilder
    ) {
        this.portForwardDecision = portForwardDecision;
        this.k8sPortForwarder = k8sPortForwarder;
        this.passwordProvider = passwordProvider;
        this.urlBuilder = urlBuilder;
    }

    /**
     * Elasticsearch 접속 정보를 결정한다.
     *
     * <p>동작 순서:
     * <ol>
     *   <li>기본 URL/계정을 읽는다.</li>
     *   <li>자동 포트포워딩 조건이 아니면 원래 URL을 반환한다.</li>
     *   <li>조건이 맞으면 kubectl 확인 → 서비스 이름 결정 → 비밀번호 로딩 → 포트포워딩 실행.</li>
     *   <li>localhost URL로 재조합해 반환한다.</li>
     *   <li>실패 시 원래 URL로 폴백한다.</li>
     * </ol>
     */
    public ConnectionInfo getConnectionInfo(AiSearchProperties properties, AiSearchK8sProperties k8sProperties) {
        URI uri = URI.create(properties.elasticsearchUrl());
        String username = properties.username();
        String password = properties.password();

        if (!portForwardDecision.shouldAutoForward(k8sProperties, uri)) {
            return new ConnectionInfo(uri.toString(), username, password);
        }

        try {
            k8sPortForwarder.requireKubectl();

            String namespace = k8sProperties.namespace();
            String serviceName = k8sPortForwarder.resolveServiceName(namespace, k8sProperties.serviceName());

            password = passwordProvider.resolvePassword(namespace, k8sProperties.secretName(), password);

            k8sPortForwarder.ensurePortForward(
                    namespace,
                    serviceName,
                    k8sProperties.localPort(),
                    k8sProperties.remotePort()
            );
            String resolvedUrl = urlBuilder.buildLocalUrl(uri, k8sProperties.localPort());
            return new ConnectionInfo(resolvedUrl, username, password);
        } catch (Exception e) {
            log.warn("[ES_AUTO] port-forward failed, falling back to {}", uri, e);
            return new ConnectionInfo(uri.toString(), username, password);
        }
    }

    /**
     * 최종 접속 정보를 담는 불변 레코드.
     */
    public record ConnectionInfo(String url, String username, String password) {
    }
}
