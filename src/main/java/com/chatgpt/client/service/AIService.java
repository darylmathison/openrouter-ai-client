package com.chatgpt.client.service;

import com.chatgpt.client.dto.ChatRequest;
import com.chatgpt.client.dto.ChatResponse;
import com.chatgpt.client.dto.TokenUsage;
import com.chatgpt.client.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AIService {

  private final WebClient webClient;
  private final CostCalculationService costCalculationService;
  private final ObjectMapper objectMapper;

  @Value("${openai.default.model:gpt-3.5-turbo}")
  private String defaultModel;

  @Value("${openai.default.max-tokens:1000}")
  private Integer defaultMaxTokens;

  @Value("${openai.default.temperature:0.7}")
  private Double defaultTemperature;

  public AIService(WebClient openRouterWebClient,
      CostCalculationService costCalculationService) {
    this.webClient = openRouterWebClient;
    this.costCalculationService = costCalculationService;
    this.objectMapper = new ObjectMapper();
  }

  public Mono<ChatResponse> sendChatRequest(ChatRequest request) {
    return Mono.fromCallable(() -> {
      try {
        List<JsonNode> messages = convertToJsonMessages(request.getMessages());

        // Add system message if present
        if (request.getSystemMessage() != null && !request.getSystemMessage().trim().isEmpty()) {
          ObjectNode systemMessage = objectMapper.createObjectNode();
          systemMessage.put("role", "system");
          systemMessage.put("content", request.getSystemMessage());
          messages.addFirst(systemMessage);
        }

        String model = request.getModel() != null ? request.getModel() : defaultModel;
        Integer maxTokens =
            request.getMaxTokens() != null ? request.getMaxTokens() : defaultMaxTokens;
        Double temperature =
            request.getTemperature() != null ? request.getTemperature() : defaultTemperature;

        // Create request body
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        ArrayNode messagesNode = requestBody.putArray("messages");
        messages.forEach(messagesNode::add);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody.toString())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
              String responseContent = response.path("choices").path(0).path("message")
                  .path("content").asText();

              TokenUsage tokenUsage = TokenUsage.builder()
                  .promptTokens(response.path("usage").path("prompt_tokens").asInt())
                  .completionTokens(response.path("usage").path("completion_tokens").asInt())
                  .totalTokens(response.path("usage").path("total_tokens").asInt())
                  .build();

              Double estimatedCost = costCalculationService.calculateCost(
                  model,
                  tokenUsage.getPromptTokens(),
                  tokenUsage.getCompletionTokens()
              );

              return ChatResponse.builder()
                  .content(responseContent)
                  .model(model)
                  .temperature(temperature)
                  .tokenUsage(tokenUsage)
                  .estimatedCost(estimatedCost)
                  .build();
            })
            .onErrorResume(e -> {
              log.error("Error calling OpenRouter API", e);
              return Mono.error(new RuntimeException("Failed to get response from OpenRouter", e));
            });
      } catch (Exception e) {
        log.error("Error preparing OpenRouter API request", e);
        throw new RuntimeException("Failed to prepare request for OpenRouter", e);
      }
    }).flatMap(mono -> mono);
  }

  public Mono<String> generateImage(String prompt, String size, Integer n) {
    return Mono.fromCallable(() -> {
      try {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("prompt", prompt);
        requestBody.put("size", size != null ? size : "1024x1024");
        requestBody.put("n", n != null ? n : 1);

        return webClient.post()
            .uri("/images/generations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody.toString())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> response.path("data").path(0).path("url").asText())
            .onErrorResume(e -> {
              log.error("Error generating image with OpenRouter", e);
              return Mono.error(new RuntimeException("Failed to generate image", e));
            });
      } catch (Exception e) {
        log.error("Error preparing image generation request", e);
        throw new RuntimeException("Failed to prepare image generation request", e);
      }
    }).flatMap(mono -> mono);
  }

  public Double estimateCost(String model, List<Message> messages) {
    int promptTokens = messages.stream()
        .mapToInt(msg -> estimateTokenCount(msg.getContent()))
        .sum();

    return costCalculationService.calculateCost(model, promptTokens, defaultMaxTokens);
  }

  private List<JsonNode> convertToJsonMessages(List<Message> messages) {
    List<JsonNode> jsonMessages = new ArrayList<>();

    for (Message msg : messages) {
      ObjectNode jsonMessage = objectMapper.createObjectNode();
      jsonMessage.put("role", msg.getRole().toString().toLowerCase());
      jsonMessage.put("content", msg.getContent());
      jsonMessages.add(jsonMessage);
    }

    return jsonMessages;
  }

  private int estimateTokenCount(String text) {
    // Simple estimation: ~4 characters per token
    return (int) Math.ceil(text.length() / 4.0);
  }

  public Mono<List<String>> getAvailableModels() {
    return webClient.get()
        .uri("/models")
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(response -> {
          List<String> models = new ArrayList<>();
          JsonNode data = response.path("data");
          if (data.isArray()) {
            for (JsonNode model : data) {
              models.add(model.path("id").asText());
            }
          }
          models.sort(String::compareToIgnoreCase);
          return models;
        })
        .onErrorResume(e -> {
          log.error("Error fetching available models from OpenRouter", e);
          return Mono.just(List.of(
              "openai/gpt-3.5-turbo",
              "openai/gpt-4",
              "anthropic/claude-3-opus",
              "anthropic/claude-3-sonnet",
              "google/gemini-pro"
          ));
        });
  }
}
