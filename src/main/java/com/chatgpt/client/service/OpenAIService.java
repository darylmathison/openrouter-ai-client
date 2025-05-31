package com.chatgpt.client.service;

import com.chatgpt.client.dto.ChatRequest;
import com.chatgpt.client.dto.ChatResponse;
import com.chatgpt.client.dto.TokenUsage;
import com.chatgpt.client.model.Message;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OpenAIService {

  private final OpenAiService openAiService;
  private final CostCalculationService costCalculationService;

  @Value("${openai.default.model:gpt-3.5-turbo}")
  private String defaultModel;

  @Value("${openai.default.max-tokens:1000}")
  private Integer defaultMaxTokens;

  @Value("${openai.default.temperature:0.7}")
  private Double defaultTemperature;

  public OpenAIService(@Value("${openai.api.key}") String apiKey,
      CostCalculationService costCalculationService) {
    this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
    this.costCalculationService = costCalculationService;
  }

  public Mono<ChatResponse> sendChatRequest(ChatRequest request) {
    return Mono.fromCallable(() -> {
      try {
        List<ChatMessage> messages = convertToChatMessages(request.getMessages());

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
            .model(request.getModel() != null ? request.getModel() : defaultModel)
            .messages(messages)
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : defaultMaxTokens)
            .temperature(
                request.getTemperature() != null ? request.getTemperature() : defaultTemperature)
            .build();

        ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);

        String responseContent = result.getChoices().getFirst().getMessage().getContent();
        TokenUsage tokenUsage = TokenUsage.builder()
            .promptTokens(Long.valueOf(result.getUsage().getPromptTokens()).intValue())
            .completionTokens(Long.valueOf(result.getUsage().getCompletionTokens()).intValue())
            .totalTokens(Long.valueOf(result.getUsage().getTotalTokens()).intValue())
            .build();

        Double estimatedCost = costCalculationService.calculateCost(
            completionRequest.getModel(),
            tokenUsage.getPromptTokens(),
            tokenUsage.getCompletionTokens()
        );

        return ChatResponse.builder()
            .content(responseContent)
            .model(completionRequest.getModel())
            .tokenUsage(tokenUsage)
            .estimatedCost(estimatedCost)
            .build();

      } catch (Exception e) {
        log.error("Error calling OpenAI API", e);
        throw new RuntimeException("Failed to get response from OpenAI", e);
      }
    });
  }

  public Mono<String> generateImage(String prompt, String size, Integer n) {
    return Mono.fromCallable(() -> {
      try {
        CreateImageRequest imageRequest = CreateImageRequest.builder()
            .prompt(prompt)
            .size(size != null ? size : "1024x1024")
            .n(n != null ? n : 1)
            .build();

        ImageResult result = openAiService.createImage(imageRequest);
        return result.getData().get(0).getUrl();

      } catch (Exception e) {
        log.error("Error generating image with OpenAI", e);
        throw new RuntimeException("Failed to generate image", e);
      }
    });
  }

  public Double estimateCost(String model, List<Message> messages) {
    int promptTokens = messages.stream()
        .mapToInt(msg -> estimateTokenCount(msg.getContent()))
        .sum();

    return costCalculationService.calculateCost(model, promptTokens, defaultMaxTokens);
  }

  private List<ChatMessage> convertToChatMessages(List<Message> messages) {
    return messages.stream()
        .map(msg -> new ChatMessage(
            ChatMessageRole.valueOf(msg.getRole().toString()).value().toLowerCase(),
            msg.getContent()
        ))
        .collect(Collectors.toList());
  }

  private int estimateTokenCount(String text) {
    // Simple estimation: ~4 characters per token
    return (int) Math.ceil(text.length() / 4.0);
  }
}
