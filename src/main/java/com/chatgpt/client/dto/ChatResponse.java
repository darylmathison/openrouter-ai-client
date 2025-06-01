package com.chatgpt.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ChatResponse {

  private Long chatId;
  private String content;
  private String model;
  private Double temperature;
  private TokenUsage tokenUsage;
  private Double estimatedCost;
  private String generatedPrompt;
  private Long messageId;
}
