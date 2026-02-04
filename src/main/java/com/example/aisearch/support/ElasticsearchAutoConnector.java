package com.example.aisearch.support;

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

    public ConnectionInfo resolve(AiSearchProperties properties) {
        URI uri = URI.create(properties.getElasticsearchUrl());
        String username = properties.getUsername();
        String password = properties.getPassword();

        if (!properties.isAutoPortForward() || !isLocalHost(uri.getHost())) {
            return new ConnectionInfo(uri.toString(), username, password);
        }

        try {
            ElasticsearchK8sHelper.requireKubectl();

            String namespace = properties.getK8sNamespace();
            String serviceName = properties.getK8sServiceName();
            if (serviceName == null || serviceName.isBlank()) {
                serviceName = ElasticsearchK8sHelper.findEsHttpService(namespace);
            }

            if (password == null || password.isBlank() || "password".equals(password)) {
                password = ElasticsearchK8sHelper.readElasticPassword(
                        namespace,
                        properties.getK8sSecretName()
                );
                log.info("[ES_AUTO] elastic password loaded from secret {}", properties.getK8sSecretName());
            }

            ensurePortForward(namespace, serviceName, properties.getK8sLocalPort(), properties.getK8sRemotePort());
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            String resolvedUrl = scheme + "://localhost:" + properties.getK8sLocalPort();
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
