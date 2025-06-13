package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for creating and managing the Weather API tool.
 * This tool allows users to get weather information for a specified city.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final ExternalToolRepository externalToolRepository;
    private final ObjectMapper objectMapper;

    @Value("${openweather.api.key:}")
    private String openWeatherApiKey;

    /**
     * Initialize the Weather API tool when the service starts.
     * This ensures that the tool is available for use in the chat interface.
     */
    @PostConstruct
    public void initializeWeatherTool() {
        if (openWeatherApiKey == null || openWeatherApiKey.isEmpty()) {
            log.warn("OpenWeather API key not configured. Weather tool will be created but may not function properly.");
        }

        externalToolRepository.findByNameIgnoreCase("Weather")
            .switchIfEmpty(createWeatherTool())
            .subscribe(
                tool -> log.info("Weather tool is ready: {}", tool.getName()),
                error -> log.error("Failed to initialize Weather tool: {}", error.getMessage())
            );
    }

    /**
     * Create the Weather API tool.
     * 
     * @return A Mono containing the created tool
     */
    private Mono<ExternalTool> createWeatherTool() {
        log.info("Creating Weather API tool");

        ExternalTool weatherTool = new ExternalTool();
        weatherTool.setName("Weather");
        weatherTool.setDescription("Get current weather information for any city");
        weatherTool.setEndpointUrl("https://api.openweathermap.org/data/2.5/weather");
        weatherTool.setHttpMethod(ExternalTool.HttpMethod.GET);
        weatherTool.setAuthType(ExternalTool.AuthType.API_KEY);

        try {
            // Configure authentication
            String authConfig = objectMapper.writeValueAsString(
                Map.of("apiKey", openWeatherApiKey, "headerName", "X-API-Key")
            );
            weatherTool.setAuthConfig(authConfig);

            // Configure request template
            weatherTool.setRequestTemplate("?q={{city}}&appid=" + openWeatherApiKey + "&units=metric");

            // Configure response mapping
            String responseMapping = objectMapper.writeValueAsString(
                Map.of(
                    "content_path", ".",
                    "error_path", "message"
                )
            );
            weatherTool.setResponseMapping(responseMapping);

            weatherTool.setToolType("API");
            weatherTool.setIsMcpEnabled(true);
            weatherTool.setCreatedAt(LocalDateTime.now());
            weatherTool.setUpdatedAt(LocalDateTime.now());
            weatherTool.setIsActive(true);

            return externalToolRepository.save(weatherTool);
        } catch (Exception e) {
            log.error("Error creating Weather tool: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    /**
     * Get the temperature for a city.
     * 
     * @param city The name of the city
     * @return A Mono containing the temperature in Celsius
     */
    public Mono<Double> getTemperatureForCity(String city) {
        if (openWeatherApiKey == null || openWeatherApiKey.isEmpty()) {
            log.warn("OpenWeather API key not configured. Cannot get temperature for city: {}", city);
            return Mono.error(new IllegalStateException("OpenWeather API key not configured"));
        }

        return WebClient.create()
            .get()
            .uri("https://api.openweathermap.org/data/2.5/weather?q={city}&appid={apiKey}&units=metric", 
                 city, openWeatherApiKey)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                Map<String, Object> main = (Map<String, Object>) response.get("main");
                return (Double) main.get("temp");
            })
            .onErrorResume(e -> {
                log.error("Error getting temperature for city {}: {}", city, e.getMessage());
                return Mono.error(e);
            });
    }
}
