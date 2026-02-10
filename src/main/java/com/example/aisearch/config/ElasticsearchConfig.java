package com.example.aisearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import com.example.aisearch.support.ElasticsearchAutoConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient restClient(
            AiSearchProperties properties,
            AiSearchK8sProperties k8sProperties,
            ElasticsearchAutoConnector autoConnector
    ) {
        ElasticsearchAutoConnector.ConnectionInfo info = autoConnector.resolve(properties, k8sProperties);
        // URL에서 호스트/포트/프로토콜을 분리
        URI uri = URI.create(info.url());

        // 기본 인증(사용자/비밀번호) 설정
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(info.username(), info.password())
        );

        // Elasticsearch 저수준 REST 클라이언트 생성
        return RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();
    }

    @Bean(destroyMethod = "close")
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        // Java API Client에서 사용할 전송 계층
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        // 고수준 Elasticsearch Java Client
        return new ElasticsearchClient(transport);
    }
}
