package com.darylmathison.chat.client.controller;

import static org.mockito.Mockito.when;

import com.darylmathison.chat.client.service.AIService;
import com.darylmathison.chat.client.service.ChatService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ModelControllerTest {

  @Mock
  private AIService aiService;

  @Mock
  private ChatService chatService;

  private ModelController modelController;

  @BeforeEach
  void setUp() {
    modelController = new ModelController(aiService, chatService);
    // Set initialCredits via reflection since it's injected via @Value
    try {
      java.lang.reflect.Field field = ModelController.class.getDeclaredField("initialCredits");
      field.setAccessible(true);
      field.set(modelController, 10.0);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set initialCredits", e);
    }
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
                response.getBody() != null &&
                !response.getBody().isEmpty())
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

  @Test
  void getRemainingCredits_ReturnsCorrectCredits() {
    // Given
    Double totalCost = 3.5;
    Double actualCredits = 8.2; // Mock actual credits from OpenRouter
    when(chatService.getTotalCostThisMonth()).thenReturn(Mono.just(totalCost));
    when(aiService.getCreditBalance()).thenReturn(Mono.just(actualCredits));

    // When & Then
    StepVerifier.create(modelController.getRemainingCredits())
        .expectNextMatches(response -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            Map<String, Object> body = response.getBody();
            return body.get("initialCredits").equals(10.0) &&
                   body.get("totalCost").equals(totalCost) &&
                   body.get("calculatedRemainingCredits").equals(6.5) &&
                   body.get("actualCredits").equals(actualCredits);
        })
        .verifyComplete();
  }
}
