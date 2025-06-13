package com.darylmathison.chat.client.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.darylmathison.chat.client.dto.ChatRequest;
import com.darylmathison.chat.client.model.Message;
import com.darylmathison.chat.client.model.Message.MessageRole;
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

    // Set defaultMaxTokens via reflection since it's injected via @Value
    try {
      java.lang.reflect.Field field = AIService.class.getDeclaredField("defaultMaxTokens");
      field.setAccessible(true);
      field.set(aiService, 4000);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set defaultMaxTokens", e);
    }
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

  @Test
  void sendChatRequest_UsesDefaultMaxTokens_WhenNotSpecified() {
    // Given
    Message message = new Message();
    message.setRole(MessageRole.USER);
    message.setContent("Test message");

    ChatRequest chatRequest = ChatRequest.builder()
        .messages(List.of(message))
        .model("openai/gpt-4")
        // Not specifying maxTokens, should use default value
        .build();

    // Setup WebClient mock chain for POST request
    when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
    when(requestBodyUriSpecMock.uri("/chat/completions")).thenReturn(requestBodySpecMock);
    when(requestBodySpecMock.contentType(org.springframework.http.MediaType.APPLICATION_JSON)).thenReturn(requestBodySpecMock);

    // Capture the request body to verify it contains the default maxTokens
    org.mockito.ArgumentCaptor<String> requestBodyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    when(requestBodySpecMock.bodyValue(requestBodyCaptor.capture())).thenReturn(requestHeadersSpecMock);

    // Mock the response
    when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);

    // Create a sample response
    ObjectNode responseJson = objectMapper.createObjectNode();
    ObjectNode choiceNode = objectMapper.createObjectNode();
    ObjectNode messageNode = objectMapper.createObjectNode();
    messageNode.put("content", "Test response");
    choiceNode.set("message", messageNode);
    ArrayNode choicesArray = responseJson.putArray("choices");
    choicesArray.add(choiceNode);

    ObjectNode usageNode = objectMapper.createObjectNode();
    usageNode.put("prompt_tokens", 10);
    usageNode.put("completion_tokens", 20);
    usageNode.put("total_tokens", 30);
    responseJson.set("usage", usageNode);

    when(responseSpecMock.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseJson));

    // Mock cost calculation
    when(costCalculationService.calculateCost(org.mockito.ArgumentMatchers.anyString(), 
                                             org.mockito.ArgumentMatchers.anyInt(), 
                                             org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(0.01);

    // When
    aiService.sendChatRequest(chatRequest).block();

    // Then
    String capturedRequestBody = requestBodyCaptor.getValue();
    JsonNode requestBodyJson = null;
    try {
        requestBodyJson = objectMapper.readTree(capturedRequestBody);
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse request body", e);
    }

    // Verify the request body contains the default maxTokens value
    assert requestBodyJson.has("max_tokens") : "Request body should have max_tokens field";
    assert requestBodyJson.get("max_tokens").asInt() == 4000 : "max_tokens should be 4000";
  }
}
