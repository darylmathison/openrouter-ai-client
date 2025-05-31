package com.chatgpt.client.service;

import com.chatgpt.client.dto.ChatRequest;
import com.chatgpt.client.dto.ChatResponse;
import com.chatgpt.client.dto.SavedPromptDto;
import com.chatgpt.client.model.Message;
import com.chatgpt.client.model.SavedPrompt;
import com.chatgpt.client.repository.SavedPromptRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedPromptService {

  private final SavedPromptRepository savedPromptRepository;
  private final ChatService chatService;

  public Mono<SavedPromptDto> savePrompt(SavedPromptDto promptDto) {
    return Mono.just(promptDto)
        .map(dto -> SavedPrompt.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .prompt(dto.getPrompt())
            .systemMessage(dto.getSystemMessage())
            .modelName(dto.getModelName())
            .maxTokens(dto.getMaxTokens())
            .temperature(dto.getTemperature())
            .category(dto.getCategory())
            .tags(dto.getTags())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .usageCount(0L)
            .isFavorite(false)
            .build())
        .flatMap(savedPromptRepository::save)
        .map(this::convertToDto)
        .doOnSuccess(saved -> log.info("Saved prompt: {}", saved.getName()))
        .doOnError(error -> log.error("Error saving prompt: {}", error.getMessage()));
  }

  public Mono<ChatResponse> executePrompt(Long promptId) {
    return savedPromptRepository.findById(promptId)
        .switchIfEmpty(Mono.error(new RuntimeException("Prompt not found with id: " + promptId)))
        .flatMap(prompt -> {
            // Increment usage count
            prompt.incrementUsage();
            return savedPromptRepository.save(prompt)
                .then(Mono.just(prompt));
        })
        .flatMap(prompt -> {
            // Create a ChatRequest from the saved prompt
            Message userMessage = Message.builder()
                .content(prompt.getPrompt())
                .role(Message.MessageRole.USER)
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(userMessage))
                .model(prompt.getModelName() != null ? prompt.getModelName() : "gpt-3.5-turbo")
                .maxTokens(prompt.getMaxTokens())
                .temperature(prompt.getTemperature())
                .systemMessage(prompt.getSystemMessage())
                .build();

            // Use ChatService to create a proper chat conversation
            // This will create a new chat, save messages, and return a proper ChatResponse with chatId
            return chatService.sendMessage(null, chatRequest)
                .map(response -> response.toBuilder()
                    .generatedPrompt(prompt.getPrompt()) // Add the original prompt content for frontend
                    .build());
        })
        .doOnSuccess(response -> log.info("Executed prompt {} and created chat {}", promptId, response.getChatId()))
        .doOnError(error -> log.error("Error executing prompt {}: {}", promptId, error.getMessage()));
}



  public Mono<List<SavedPromptDto>> getAllPrompts() {
    return savedPromptRepository.findAllByOrderByUsageCountDesc()
        .map(this::convertToDto)
        .collectList()
        .doOnSuccess(prompts -> log.info("Retrieved {} prompts", prompts.size()));
  }

  public Mono<List<SavedPromptDto>> searchPrompts(String searchTerm) {
    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return getAllPrompts();
    }

    return savedPromptRepository.findByContentContaining(searchTerm.trim())
        .map(this::convertToDto)
        .collectList()
        .doOnSuccess(prompts -> log.info("Found {} prompts for search term: {}", prompts.size(),
            searchTerm));
  }

  public Mono<SavedPromptDto> updatePrompt(Long promptId, SavedPromptDto promptDto) {
    return savedPromptRepository.findById(promptId)
        .switchIfEmpty(Mono.error(new RuntimeException("Prompt not found with id: " + promptId)))
        .map(existingPrompt -> {
          existingPrompt.setName(promptDto.getName());
          existingPrompt.setDescription(promptDto.getDescription());
          existingPrompt.setPrompt(promptDto.getPrompt());
          existingPrompt.setSystemMessage(promptDto.getSystemMessage());
          existingPrompt.setModelName(promptDto.getModelName());
          existingPrompt.setMaxTokens(promptDto.getMaxTokens());
          existingPrompt.setTemperature(promptDto.getTemperature());
          existingPrompt.setCategory(promptDto.getCategory());
          existingPrompt.setTags(promptDto.getTags());
          if (promptDto.getIsFavorite() != null) {
            existingPrompt.setIsFavorite(promptDto.getIsFavorite());
          }
          existingPrompt.updateTimestamp();
          return existingPrompt;
        })
        .flatMap(savedPromptRepository::save)
        .map(this::convertToDto)
        .doOnSuccess(updated -> log.info("Updated prompt: {}", updated.getName()))
        .doOnError(
            error -> log.error("Error updating prompt {}: {}", promptId, error.getMessage()));
  }

  public Mono<Void> deletePrompt(Long promptId) {
    return savedPromptRepository.findById(promptId)
        .switchIfEmpty(Mono.error(new RuntimeException("Prompt not found with id: " + promptId)))
        .flatMap(prompt -> savedPromptRepository.deleteById(promptId))
        .doOnSuccess(v -> log.info("Deleted prompt with id: {}", promptId))
        .doOnError(
            error -> log.error("Error deleting prompt {}: {}", promptId, error.getMessage()));
  }

  public Mono<Double> estimatePromptCost(Long promptId) {
    return savedPromptRepository.findById(promptId)
        .switchIfEmpty(Mono.error(new RuntimeException("Prompt not found with id: " + promptId)))
        .map(prompt -> {
          // Simple cost estimation - you can enhance this
          int estimatedTokens = (prompt.getPrompt() != null ? prompt.getPrompt().length() / 4 : 0) +
              (prompt.getSystemMessage() != null ? prompt.getSystemMessage().length() / 4 : 0);

          // Rough cost estimation (adjust based on your model pricing)
          double costPerToken = 0.000002; // Example rate
          return estimatedTokens * costPerToken;
        })
        .doOnSuccess(cost -> log.info("Estimated cost for prompt {}: ${}", promptId, cost))
        .doOnError(error -> log.error("Error estimating cost for prompt {}: {}", promptId,
            error.getMessage()));
  }

  private ChatRequest convertSavedPromptToChatRequest(SavedPrompt savedPrompt) {
    List<Message> messages = new ArrayList<>();

    // Add system message if present
    if (savedPrompt.getSystemMessage() != null && !savedPrompt.getSystemMessage().trim()
        .isEmpty()) {
      messages.add(Message.builder()
          .role(Message.MessageRole.SYSTEM)
          .content(savedPrompt.getSystemMessage())
          .build());
    }

    // Add the main prompt as user message
    messages.add(Message.builder()
        .role(Message.MessageRole.SYSTEM)
        .content(savedPrompt.getPrompt())
        .build());

    return ChatRequest.builder()
        .messages(messages)
        .model(savedPrompt.getModelName())
        .maxTokens(savedPrompt.getMaxTokens())
        .temperature(savedPrompt.getTemperature())
        .build();
  }


  private SavedPromptDto convertToDto(SavedPrompt prompt) {
    return SavedPromptDto.builder()
        .id(prompt.getId())
        .name(prompt.getName())
        .description(prompt.getDescription())
        .prompt(prompt.getPrompt())
        .systemMessage(prompt.getSystemMessage())
        .modelName(prompt.getModelName())
        .maxTokens(prompt.getMaxTokens())
        .temperature(prompt.getTemperature())
        .category(prompt.getCategory())
        .usageCount(prompt.getUsageCount())
        .createdAt(prompt.getCreatedAt())
        .updatedAt(prompt.getUpdatedAt())
        .isFavorite(prompt.getIsFavorite())
        .tags(prompt.getTags())
        .build();
  }
}