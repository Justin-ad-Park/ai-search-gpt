package com.example.aisearch.support;

import com.example.aisearch.config.AiSearchK8sProperties;
import com.example.aisearch.config.AiSearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ElasticsearchAutoConnector {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchAutoConnector.class);

    private final PortForwardDecision portForwardDecision;
    private final K8sPortForwarder k8sPortForwarder;
    private final ElasticPasswordProvider passwordProvider;
    private final ElasticsearchUrlBuilder urlBuilder;

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

    public ConnectionInfo resolve(AiSearchProperties properties, AiSearchK8sProperties k8sProperties) {
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

    public record ConnectionInfo(String url, String username, String password) {
    }
}
