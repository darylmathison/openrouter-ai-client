package com.chatgpt.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private CostCalculationService costCalculationService;

    private WebClient webClientMock;
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock;
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    private WebClient.ResponseSpec responseSpecMock;

    private AIService aiService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Create mocks for WebClient chain
        webClientMock = mock(WebClient.class);
        requestHeadersUriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        responseSpecMock = mock(WebClient.ResponseSpec.class);

        // Create real ObjectMapper for JSON manipulation
        objectMapper = new ObjectMapper();

        // Create AIService with mocked WebClient
        aiService = new AIService(webClientMock, costCalculationService);
    }

    @Test
    void getAvailableModels_Success_ReturnsModelsList() {
        // Given
        // Create a sample response JSON
        ObjectNode responseJson = objectMapper.createObjectNode();
        ArrayNode dataArray = responseJson.putArray("data");

        ObjectNode model1 = dataArray.addObject();
        model1.put("id", "openai/gpt-3.5-turbo");

        ObjectNode model2 = dataArray.addObject();
        model2.put("id", "openai/gpt-4");

        ObjectNode model3 = dataArray.addObject();
        model3.put("id", "anthropic/claude-3-opus");

        // Setup WebClient mock chain
        when(webClientMock.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri("/models")).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseJson));

        // When & Then
        StepVerifier.create(aiService.getAvailableModels())
            .expectNextMatches(models -> 
                models.size() == 3 &&
                models.contains("openai/gpt-3.5-turbo") &&
                models.contains("openai/gpt-4") &&
                models.contains("anthropic/claude-3-opus"))
            .verifyComplete();
    }

    @Test
    void getAvailableModels_Error_ReturnsFallbackModels() {
        // Given
        // Setup WebClient mock chain to throw an error
        when(webClientMock.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri("/models")).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(JsonNode.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

        // When & Then
        StepVerifier.create(aiService.getAvailableModels())
            .expectNextMatches(models -> 
                models.size() == 5 &&
                models.contains("openai/gpt-3.5-turbo") &&
                models.contains("openai/gpt-4") &&
                models.contains("anthropic/claude-3-opus") &&
                models.contains("anthropic/claude-3-sonnet") &&
                models.contains("google/gemini-pro"))
            .verifyComplete();
    }
}
