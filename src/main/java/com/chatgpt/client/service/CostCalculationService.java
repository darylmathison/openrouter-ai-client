package com.chatgpt.client.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CostCalculationService {

  // Pricing per 1K tokens (as of 2024 - these should be configurable)
  private static final Map<String, ModelPricing> MODEL_PRICING = Map.of(
      "gpt-3.5-turbo", new ModelPricing(0.0015, 0.002),
      "gpt-4", new ModelPricing(0.03, 0.06),
      "gpt-4-turbo", new ModelPricing(0.01, 0.03),
      "gpt-4o", new ModelPricing(0.005, 0.015),
      "gpt-4o-mini", new ModelPricing(0.00015, 0.0006)
  );

  public Double calculateCost(String model, int promptTokens, int completionTokens) {
    ModelPricing pricing = MODEL_PRICING.getOrDefault(model, MODEL_PRICING.get("gpt-3.5-turbo"));

    double promptCost = (promptTokens / 1000.0) * pricing.promptPrice;
    double completionCost = (completionTokens / 1000.0) * pricing.completionPrice;

    return promptCost + completionCost;
  }

  public Double estimateCost(String model, Integer estimatedTokens) {
    ModelPricing pricing = MODEL_PRICING.getOrDefault(model, MODEL_PRICING.get("gpt-3.5-turbo"));

    // Assume 70% prompt, 30% completion for estimation
    double promptTokens = estimatedTokens * 0.7;
    double completionTokens = estimatedTokens * 0.3;

    double promptCost = (promptTokens / 1000.0) * pricing.promptPrice;
    double completionCost = (completionTokens / 1000.0) * pricing.completionPrice;

    return promptCost + completionCost;
  }

  private static class ModelPricing {

    final double promptPrice;
    final double completionPrice;

    ModelPricing(double promptPrice, double completionPrice) {
      this.promptPrice = promptPrice;
      this.completionPrice = completionPrice;
    }
  }
}
