package com.chatgpt.client.dto;

import com.chatgpt.client.model.Message;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatRequest {

  private String message;

  @NotNull(message = "Messages list cannot be null")
  @NotEmpty(message = "Messages list cannot be empty")
  private List<Message> messages;

  private String model;
  private Integer maxTokens;
  private Double temperature;
  private String systemMessage;
  private List<Long> attachmentIds;
  private List<Long> externalToolIds;
}