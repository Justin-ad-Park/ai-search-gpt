package com.example.aisearch.integration.helper;

public abstract class TruststoreTestBase {

    static {
        System.setProperty(
                "javax.net.ssl.trustStore",
                System.getProperty("user.home") + "/.ai-cert/djl-truststore.p12"
        );
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }
}
