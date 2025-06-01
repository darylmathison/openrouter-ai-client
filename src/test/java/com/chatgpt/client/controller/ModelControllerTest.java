package com.chatgpt.client.controller;

import static org.mockito.Mockito.when;

import com.chatgpt.client.service.AIService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ModelControllerTest {

    @Mock
    private AIService aiService;

    private ModelController modelController;

    @BeforeEach
    void setUp() {
        modelController = new ModelController(aiService);
    }

    @Test
    void getAvailableModels_ReturnsModelsFromAIService() {
        // Given
        List<String> expectedModels = List.of(
            "openai/gpt-3.5-turbo",
            "openai/gpt-4",
            "anthropic/claude-3-opus",
            "anthropic/claude-3-sonnet",
            "google/gemini-pro"
        );
        when(aiService.getAvailableModels()).thenReturn(Mono.just(expectedModels));

        // When & Then
        StepVerifier.create(modelController.getAvailableModels())
            .expectNextMatches(response -> 
                response.getStatusCode().is2xxSuccessful() &&
                response.getBody().equals(expectedModels))
            .verifyComplete();
    }

    @Test
    void getSelectedModels_ReturnsSelectedModels() {
        // When & Then
        StepVerifier.create(modelController.getSelectedModels())
            .expectNextMatches(response -> 
                response.getStatusCode().is2xxSuccessful() &&
                response.getBody() instanceof Set &&
                response.getBody().size() > 0)
            .verifyComplete();
    }

    @Test
    void updateSelectedModels_WithValidModels_UpdatesSelectedModels() {
        // Given
        List<String> newModels = List.of(
            "openai/gpt-4",
            "anthropic/claude-3-opus"
        );

        // When & Then
        StepVerifier.create(modelController.updateSelectedModels(newModels))
            .expectNextMatches(response -> 
                response.getStatusCode().is2xxSuccessful() &&
                response.getBody().size() == 2 &&
                response.getBody().contains("openai/gpt-4") &&
                response.getBody().contains("anthropic/claude-3-opus"))
            .verifyComplete();
    }

    @Test
    void updateSelectedModels_WithTooManyModels_ReturnsBadRequest() {
        // Given
        List<String> tooManyModels = List.of(
            "model1", "model2", "model3", "model4", "model5", "model6"
        );

        // When & Then
        StepVerifier.create(modelController.updateSelectedModels(tooManyModels))
            .expectNextMatches(response -> response.getStatusCode().is4xxClientError())
            .verifyComplete();
    }
}