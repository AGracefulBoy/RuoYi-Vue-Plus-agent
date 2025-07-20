package org.dromara.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Embedding service configuration.
 * Provides HTTP client configuration for embedding API calls.
 *
 * @author ruoyi
 */
@Configuration
public class EmbeddingConfig {

    @Value("${embedding.api.connect-timeout:30000}")
    private Integer connectTimeout;

    @Value("${embedding.api.read-timeout:60000}")
    private Integer readTimeout;

    /**
     * Creates RestTemplate bean for HTTP requests.
     *
     * @return configured RestTemplate instance
     */
    @Bean("embeddingRestTemplate")
    public RestTemplate embeddingRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Set connection timeout from configuration
        factory.setConnectTimeout(connectTimeout);
        
        // Set read timeout from configuration - embedding API might take longer
        factory.setReadTimeout(readTimeout);
        
        return new RestTemplate(factory);
    }
}