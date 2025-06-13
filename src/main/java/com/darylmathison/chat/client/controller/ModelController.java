package com.darylmathison.chat.client.controller;

import com.darylmathison.chat.client.service.AIService;
import com.darylmathison.chat.client.service.ChatService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModelController {

    private final AIService aiService;
    private final ChatService chatService;

    @Value("${openrouter.credits.initial:10.0}")
    private Double initialCredits;

    @Value("${openrouter.default.max-tokens:4000}")
    private Integer defaultMaxTokens;

    // In-memory storage for selected models (in a real app, this would be in a database)
    private static final Set<String> selectedModels = ConcurrentHashMap.newKeySet();

    // Default models if none are selected
    static {
        selectedModels.add("deepseek/deepseek-r1-0528:free");
        selectedModels.add("openai/gpt-4");
        selectedModels.add("anthropic/claude-3-opus");
        selectedModels.add("anthropic/claude-3-sonnet");
        selectedModels.add("google/gemini-pro");
    }

    @GetMapping("/available")
    public Mono<ResponseEntity<List<String>>> getAvailableModels() {
        return aiService.getAvailableModels()
            .map(ResponseEntity::ok);
    }

    @GetMapping("/selected")
    public Mono<ResponseEntity<Set<String>>> getSelectedModels() {
        return Mono.just(ResponseEntity.ok(selectedModels));
    }

    @PostMapping("/selected")
    public Mono<ResponseEntity<Set<String>>> updateSelectedModels(@RequestBody List<String> models) {
        // Ensure we don't exceed 5 models
        if (models.size() > 5) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Update selected models
        selectedModels.clear();
        selectedModels.addAll(models);

        return Mono.just(ResponseEntity.ok(selectedModels));
    }

    @GetMapping("/credits")
    public Mono<ResponseEntity<Map<String, Object>>> getRemainingCredits() {
        // Get both the actual credit balance from OpenRouter and the total cost this month
        return Mono.zip(
            aiService.getCreditBalance(),
            chatService.getTotalCostThisMonth()
        ).map(tuple -> {
            Double actualCredits = tuple.getT1();
            Double totalCost = tuple.getT2();

            // For backward compatibility, also calculate the remaining credits based on initial credits
            double calculatedRemaining = initialCredits - totalCost;
            if (calculatedRemaining < 0) calculatedRemaining = 0.0;

            Map<String, Object> response = Map.of(
                "initialCredits", initialCredits,
                "totalCost", totalCost,
                "calculatedRemainingCredits", calculatedRemaining,
                "actualCredits", actualCredits
            );

            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/config")
    public Mono<ResponseEntity<Map<String, Object>>> getModelConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxTokens", defaultMaxTokens);

        return Mono.just(ResponseEntity.ok(config));
    }
}
