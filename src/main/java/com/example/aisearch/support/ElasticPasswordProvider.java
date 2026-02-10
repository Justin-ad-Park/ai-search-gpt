package com.example.aisearch.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElasticPasswordProvider {

    private static final Logger log = LoggerFactory.getLogger(ElasticPasswordProvider.class);

    public String resolvePassword(String namespace, String secretName, String currentPassword)
            throws Exception {
        if (!needsPassword(currentPassword)) {
            return currentPassword;
        }
        String password = ElasticsearchK8sHelper.readElasticPassword(namespace, secretName);
        log.info("[ES_AUTO] elastic password loaded from secret {}", secretName);
        return password;
    }

    private boolean needsPassword(String password) {
        return password == null || password.isBlank() || "password".equals(password);
    }
}
