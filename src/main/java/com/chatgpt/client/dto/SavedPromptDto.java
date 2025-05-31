package com.chatgpt.client.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SavedPromptDto {

  private Long id;
  private String name;
  private String description;
  private String prompt;
  private String systemMessage;
  private String modelName;
  private Integer maxTokens;
  private Double temperature;
  private String category;  // Added this field
  private String tags;      // Also added tags for consistency
  private Boolean isFavorite; // Added this field too
  private Long usageCount;  // Changed from Integer to Long to match entity
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Double estimatedCost;
}