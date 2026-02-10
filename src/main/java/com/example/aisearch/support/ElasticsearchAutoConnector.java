package com.example.aisearch.support;

import com.example.aisearch.config.AiSearchK8sProperties;
import com.example.aisearch.config.AiSearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ElasticsearchAutoConnector {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchAutoConnector.class);

    private final AtomicReference<ElasticsearchK8sHelper.PortForwardHandle> handleRef = new AtomicReference<>();

    public ConnectionInfo resolve(AiSearchProperties properties, AiSearchK8sProperties k8sProperties) {
        URI uri = URI.create(properties.elasticsearchUrl());
        String username = properties.username();
        String password = properties.password();

        if (!k8sProperties.autoPortForward() || !isLocalHost(uri.getHost())) {
            return new ConnectionInfo(uri.toString(), username, password);
        }

        try {
            ElasticsearchK8sHelper.requireKubectl();

            String namespace = k8sProperties.namespace();
            String serviceName = k8sProperties.serviceName();
            if (serviceName == null || serviceName.isBlank()) {
                serviceName = ElasticsearchK8sHelper.findEsHttpService(namespace);
            }

            if (password == null || password.isBlank() || "password".equals(password)) {
                password = ElasticsearchK8sHelper.readElasticPassword(
                        namespace,
                        k8sProperties.secretName()
                );
                log.info("[ES_AUTO] elastic password loaded from secret {}", k8sProperties.secretName());
            }

            ensurePortForward(namespace, serviceName, k8sProperties.localPort(), k8sProperties.remotePort());
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            String resolvedUrl = scheme + "://localhost:" + k8sProperties.localPort();
            log.info("[ES_AUTO] using port-forwarded url {}", resolvedUrl);
            return new ConnectionInfo(resolvedUrl, username, password);
        } catch (Exception e) {
            log.warn("[ES_AUTO] port-forward failed, falling back to {}", uri, e);
            return new ConnectionInfo(uri.toString(), username, password);
        }
    }

    private void ensurePortForward(String namespace, String serviceName, int localPort, int remotePort)
            throws Exception {
        if (handleRef.get() != null) {
            return;
        }
        ElasticsearchK8sHelper.PortForwardHandle handle =
                ElasticsearchK8sHelper.startPortForward(namespace, serviceName, localPort, remotePort);
        handleRef.compareAndSet(null, handle);
        Thread.sleep(3000L);
        log.info("[ES_AUTO] port-forward started: {} -> {}", localPort, remotePort);
    }

    private boolean isLocalHost(String host) {
        if (host == null) {
            return false;
        }
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    public record ConnectionInfo(String url, String username, String password) {
    }

    @jakarta.annotation.PreDestroy
    public void close() {
        ElasticsearchK8sHelper.PortForwardHandle handle = handleRef.getAndSet(null);
        if (handle != null) {
            handle.close();
            log.info("[ES_AUTO] port-forward closed");
        }
    }
}
