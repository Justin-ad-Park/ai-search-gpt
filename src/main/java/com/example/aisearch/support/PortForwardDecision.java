package com.example.aisearch.support;

import com.example.aisearch.config.AiSearchK8sProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class PortForwardDecision {

    public boolean shouldAutoForward(AiSearchK8sProperties k8sProperties, URI uri) {
        return k8sProperties.autoPortForward() && isLocalHost(uri.getHost());
    }

    private boolean isLocalHost(String host) {
        if (host == null) {
            return false;
        }
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }
}
