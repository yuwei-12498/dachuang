package com.citytrip.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder, LlmProperties llmProperties) {
        Duration timeout = Duration.ofSeconds(Math.max(llmProperties.getTimeoutSeconds(), 1));
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}
