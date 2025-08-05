package org.dromara.system.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;

@Configuration
public class ElasticConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Bean
    public RestClient restClient() {
        // 解析URI，支持单节点
        String[] uriArray = uris.split(",");
        HttpHost[] hosts = new HttpHost[uriArray.length];

        for (int i = 0; i < uriArray.length; i++) {
            String uri = uriArray[i].trim();
            if (uri.startsWith("https://")) {
                String hostPort = uri.substring(8);
                String[] parts = hostPort.split(":");
                hosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]), "https");
            } else if (uri.startsWith("http://")) {
                String hostPort = uri.substring(7);
                String[] parts = hostPort.split(":");
                hosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]), "http");
            } else {
                // 默认http协议
                String[] parts = uri.split(":");
                hosts[i] = new HttpHost(parts[0], Integer.parseInt(parts[1]), "http");
            }
        }

        RestClientBuilder builder = RestClient.builder(hosts);

        // 配置SSL和认证
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                // 忽略SSL证书验证
                SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();

                httpClientBuilder.setSSLContext(sslContext);
                httpClientBuilder.setSSLHostnameVerifier((hostname, session) -> true);

                // 配置用户名密码认证
                if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to configure SSL context", e);
            }
            return httpClientBuilder;
        });

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClientTransport transport = new RestClientTransport(
            restClient(),
            new JacksonJsonpMapper()
        );
        return new ElasticsearchClient(transport);
    }
}

