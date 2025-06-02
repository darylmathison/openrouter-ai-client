package com.darylmathison.chat.client.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSummaryDto {

  private Long id;
  private String title;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer messageCount;
  private Double estimatedCost;
  private String lastMessagePreview;
  private String modelUsed;
}