package com.chatgpt.client.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chatgpt.client.dto.ChatRequest;
import com.chatgpt.client.model.Message;
import com.chatgpt.client.model.Message.MessageRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

  @Mock
  private CostCalculationService costCalculationService;

  private WebClient webClientMock;
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock;
  private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
  private WebClient.RequestHeadersSpec requestHeadersSpecMock;
  private WebClient.RequestBodySpec requestBodySpecMock;
  private WebClient.ResponseSpec responseSpecMock;

  private AIService aiService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    // Create mocks for WebClient chain
    webClientMock = mock(WebClient.class);
    requestHeadersUriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
    requestBodyUriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
    requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
    requestBodySpecMock = mock(WebClient.RequestBodySpec.class);
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
    model1.put("id", "deepseek/deepseek-r1-0528:free");

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
                models.contains("deepseek/deepseek-r1-0528:free") &&
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
    when(responseSpecMock.bodyToMono(JsonNode.class)).thenReturn(
        Mono.error(new RuntimeException("API Error")));

    // When & Then
    StepVerifier.create(aiService.getAvailableModels())
        .expectNextMatches(models ->
            models.size() == 5 &&
                models.contains("deepseek/deepseek-r1-0528:free") &&
                models.contains("openai/gpt-4") &&
                models.contains("anthropic/claude-3-opus") &&
                models.contains("anthropic/claude-3-sonnet") &&
                models.contains("google/gemini-pro"))
        .verifyComplete();
  }
  @Test
  void sendChatRequest_AuthenticationError_ReturnsSpecificErrorMessage() {
    // Given
    Message message = new Message();
    message.setRole(MessageRole.USER);
    message.setContent("Test message");

    ChatRequest chatRequest = ChatRequest.builder()
        .messages(List.of(message))
        .model("openai/gpt-4")
        .build();

    // Create a 401 UNAUTHORIZED exception
    RuntimeException unauthorizedException = new RuntimeException("401 UNAUTHORIZED");

    // Setup WebClient mock chain for POST request
    when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
    when(requestBodyUriSpecMock.uri("/chat/completions")).thenReturn(requestBodySpecMock);
    when(requestBodySpecMock.contentType(org.springframework.http.MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecMock);
    when(requestBodySpecMock.bodyValue(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestHeadersSpecMock);
    when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
    when(responseSpecMock.bodyToMono(JsonNode.class)).thenReturn(Mono.error(unauthorizedException));

    // When & Then
    StepVerifier.create(aiService.sendChatRequest(chatRequest))
        .expectErrorMatches(throwable -> 
            throwable.getMessage().contains("Authentication failed with OpenRouter") &&
            throwable.getMessage().contains("OPENROUTER_API_KEY environment variable"))
        .verify();
  }
}
