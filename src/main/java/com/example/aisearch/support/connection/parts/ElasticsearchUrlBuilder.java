package com.example.aisearch.support.connection.parts;

import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ElasticsearchUrlBuilder {

    public String buildLocalUrl(URI baseUri, int localPort) {
        String scheme = baseUri.getScheme() == null ? "http" : baseUri.getScheme();
        return scheme + "://localhost:" + localPort;
    }
}
