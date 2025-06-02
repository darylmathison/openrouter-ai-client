package com.darylmathison.chat.client.config;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

    Logger logger = Logger.getLogger(WebConfig.class.getName());

    @Bean
    public WebClient openRouterWebClient(@Value("${openrouter.api.key}") String apiKey) {
        return WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("HTTP-Referer", "https://localhost") // Required by OpenRouter
            .defaultHeader("X-Title", "Custom ChatGPT Client") // Optional but recommended
            .exchangeStrategies(ExchangeStrategies.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)).build())
            .build();
    }
}
