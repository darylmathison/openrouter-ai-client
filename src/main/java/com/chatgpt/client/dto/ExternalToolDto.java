package com.chatgpt.client.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ExternalToolDto {

  private Long id;
  private String name;
  private String description;
  private String endpointUrl;
  private String httpMethod;
  private String authType;
  private String authConfig;
  private String requestTemplate;
  private String responseMapping;
  private Boolean isActive;
  private String toolType;
  private Long usageCount;
  private LocalDateTime lastUsedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Backward compatibility fields
  private String baseUrl;
  private String apiKey;
  private Boolean isEnabled;
  private String configuration;
}