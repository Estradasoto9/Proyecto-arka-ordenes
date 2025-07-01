package com.projectArka.order_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    public WebClient productWebClient(String baseUrl) {
        return webClientBuilder().baseUrl(baseUrl).build();
    }

    public WebClient userWebClient(String baseUrl) {
        return webClientBuilder().baseUrl(baseUrl).build();
    }
}