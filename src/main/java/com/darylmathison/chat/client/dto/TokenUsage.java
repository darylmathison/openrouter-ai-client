package com.darylmathison.chat.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenUsage {

  private Integer promptTokens;
  private Integer completionTokens;
  private Integer totalTokens;
}
