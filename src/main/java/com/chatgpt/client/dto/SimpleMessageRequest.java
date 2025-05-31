package com.chatgpt.client.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleMessageRequest {

  @NotBlank(message = "Message cannot be blank")
  private String message;

  private String model;
  private Integer maxTokens;
  private Double temperature;
  private String systemMessage;
  private List<Long> attachmentIds;
  private List<Long> externalToolIds;
}
