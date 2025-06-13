package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WeatherServiceTest {

    @Mock
    private ExternalToolRepository externalToolRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(weatherService, "openWeatherApiKey", "test-api-key");
    }

    @Test
    void testInitializeWeatherTool_ToolDoesNotExist() throws Exception {
        // Mock repository to return empty when finding by name
        when(externalToolRepository.findByNameIgnoreCase(anyString()))
                .thenReturn(Mono.empty());

        // Mock JSON serialization
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"apiKey\":\"test-api-key\",\"headerName\":\"X-API-Key\"}");

        // Mock repository save
        ExternalTool savedTool = new ExternalTool();
        savedTool.setId(1L);
        savedTool.setName("Weather");
        when(externalToolRepository.save(any(ExternalTool.class)))
                .thenReturn(Mono.just(savedTool));

        // Call the method
        weatherService.initializeWeatherTool();

        // Verify repository was called to find the tool
        verify(externalToolRepository).findByNameIgnoreCase("Weather");

        // Verify repository was called to save the tool
        verify(externalToolRepository).save(any(ExternalTool.class));
    }

    @Test
    void testInitializeWeatherTool_ToolAlreadyExists() {
        // Mock repository to return a tool when finding by name
        ExternalTool existingTool = new ExternalTool();
        existingTool.setId(1L);
        existingTool.setName("Weather");
        when(externalToolRepository.findByNameIgnoreCase(anyString()))
                .thenReturn(Mono.just(existingTool));

        // Call the method
        weatherService.initializeWeatherTool();

        // Verify repository was called to find the tool
        verify(externalToolRepository).findByNameIgnoreCase("Weather");

        // Verify repository was NOT called to save the tool
        verify(externalToolRepository, never()).save(any(ExternalTool.class));
    }

    @Test
    void testCreateWeatherTool() throws Exception {
        // Mock JSON serialization
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"apiKey\":\"test-api-key\",\"headerName\":\"X-API-Key\"}");

        // Mock repository save
        ExternalTool savedTool = new ExternalTool();
        savedTool.setId(1L);
        savedTool.setName("Weather");
        when(externalToolRepository.save(any(ExternalTool.class)))
                .thenReturn(Mono.just(savedTool));

        // Call the method using reflection since it's private
        Mono<ExternalTool> result = (Mono<ExternalTool>) ReflectionTestUtils.invokeMethod(
                weatherService, "createWeatherTool");

        // Verify the result
        StepVerifier.create(result)
                .expectNextMatches(tool -> tool.getName().equals("Weather"))
                .verifyComplete();

        // Verify repository was called to save the tool
        verify(externalToolRepository).save(any(ExternalTool.class));
    }

    @Test
    void testCreateWeatherTool_ErrorHandling() throws Exception {
        // Mock JSON serialization to throw exception
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Test exception"));

        // Call the method using reflection since it's private
        Mono<ExternalTool> result = (Mono<ExternalTool>) ReflectionTestUtils.invokeMethod(
                weatherService, "createWeatherTool");

        // Verify the result
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        // Verify repository was NOT called to save the tool
        verify(externalToolRepository, never()).save(any(ExternalTool.class));
    }
}