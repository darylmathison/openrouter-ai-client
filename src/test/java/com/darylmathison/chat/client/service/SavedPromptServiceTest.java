package com.darylmathison.chat.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.darylmathison.chat.client.dto.ChatResponse;
import com.darylmathison.chat.client.dto.SavedPromptDto;
import com.darylmathison.chat.client.model.SavedPrompt;
import com.darylmathison.chat.client.repository.SavedPromptRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SavedPromptServiceTest {

  @Mock
  private SavedPromptRepository savedPromptRepository;

  @Mock
  private ChatService chatService;

  private SavedPromptService savedPromptService;

  @BeforeEach
  void setUp() {
    savedPromptService = new SavedPromptService(savedPromptRepository, chatService);
  }

  @Test
  void savePrompt_ValidPrompt_SavesSuccessfully() {
    // Given
    SavedPromptDto promptDto = SavedPromptDto.builder()
        .name("Blog Post Helper")
        .description("Helps create engaging blog posts")
        .prompt("Write a blog post about {{topic}}")
        .systemMessage("You are a helpful writing assistant")
        .modelName("gpt-3.5-turbo")
        .maxTokens(1000)
        .temperature(0.7)
        .category("Writing")
        .tags("blog,writing,content")
        .build();

    SavedPrompt savedPrompt = SavedPrompt.builder()
        .id(1L)
        .name("Blog Post Helper")
        .description("Helps create engaging blog posts")
        .prompt("Write a blog post about {{topic}}")
        .systemMessage("You are a helpful writing assistant")
        .modelName("gpt-3.5-turbo")
        .maxTokens(1000)
        .temperature(0.7)
        .category("Writing")
        .tags("blog,writing,content")
        .usageCount(0L)
        .isFavorite(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(savedPromptRepository.save(any(SavedPrompt.class))).thenReturn(Mono.just(savedPrompt));

    // When & Then
    StepVerifier.create(savedPromptService.savePrompt(promptDto))
        .expectNextMatches(result ->
            result.getName().equals("Blog Post Helper") &&
                result.getId().equals(1L) &&
                result.getUsageCount().equals(0L) &&
                result.getIsFavorite().equals(false))
        .verifyComplete();
  }

  @Test
  void executePrompt_ValidPromptId_ExecutesSuccessfully() {
    // Given
    Long promptId = 1L;
    SavedPrompt savedPrompt = SavedPrompt.builder()
        .id(promptId)
        .name("Test Prompt")
        .prompt("Test prompt content")
        .systemMessage("You are helpful")
        .modelName("gpt-3.5-turbo")
        .maxTokens(100)
        .temperature(0.7)
        .build();

    ChatResponse expectedResponse = ChatResponse.builder()
        .content("Generated response")
        .model("gpt-3.5-turbo")
        .build();

    when(savedPromptRepository.findById(promptId)).thenReturn(Mono.just(savedPrompt));
    when(savedPromptRepository.save(any(SavedPrompt.class))).thenReturn(Mono.just(savedPrompt));
    when(chatService.sendMessage(any(), any())).thenReturn(Mono.just(expectedResponse));

    // When & Then
    StepVerifier.create(savedPromptService.executePrompt(promptId))
        .expectNextMatches(response ->
            response.getContent().equals("Generated response") &&
                response.getModel().equals("gpt-3.5-turbo"))
        .verifyComplete();
  }

  @Test
  void executePrompt_PromptNotFound_ThrowsException() {
    // Given
    Long promptId = 999L;
    when(savedPromptRepository.findById(promptId)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(savedPromptService.executePrompt(promptId))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getAllPrompts_ReturnsAllPrompts() {
    // Given
    SavedPrompt prompt1 = SavedPrompt.builder()
        .id(1L)
        .name("Prompt 1")
        .prompt("Test prompt 1")
        .usageCount(5L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    SavedPrompt prompt2 = SavedPrompt.builder()
        .id(2L)
        .name("Prompt 2")
        .prompt("Test prompt 2")
        .usageCount(3L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(savedPromptRepository.findAllByOrderByUsageCountDesc()).thenReturn(
        Flux.just(prompt1, prompt2));

    // When & Then
    StepVerifier.create(savedPromptService.getAllPrompts())
        .expectNextMatches(prompts ->
            prompts.size() == 2 &&
                prompts.get(0).getName().equals("Prompt 1") &&
                prompts.get(0).getUsageCount().equals(5L))
        .verifyComplete();
  }

  @Test
  void searchPrompts_WithSearchTerm_ReturnsMatchingPrompts() {
    // Given
    String searchTerm = "blog";
    SavedPrompt matchingPrompt = SavedPrompt.builder()
        .id(1L)
        .name("Blog Helper")
        .prompt("Help with blog writing")
        .build();

    when(savedPromptRepository.findByContentContaining(searchTerm))
        .thenReturn(Flux.just(matchingPrompt));

    // When & Then
    StepVerifier.create(savedPromptService.searchPrompts(searchTerm))
        .expectNextMatches(prompts ->
            prompts.size() == 1 &&
                prompts.get(0).getName().equals("Blog Helper"))
        .verifyComplete();
  }

  @Test
  void searchPrompts_EmptySearchTerm_ReturnsAllPrompts() {
    // Given
    SavedPrompt prompt = SavedPrompt.builder()
        .id(1L)
        .name("Test Prompt")
        .build();

    when(savedPromptRepository.findAllByOrderByUsageCountDesc()).thenReturn(Flux.just(prompt));

    // When & Then
    StepVerifier.create(savedPromptService.searchPrompts(""))
        .expectNextMatches(prompts -> prompts.size() == 1)
        .verifyComplete();
  }

  @Test
  void updatePrompt_ValidUpdate_UpdatesSuccessfully() {
    // Given
    Long promptId = 1L;
    SavedPromptDto updateDto = SavedPromptDto.builder()
        .name("Updated Prompt")
        .description("Updated description")
        .prompt("Updated prompt content")
        .isFavorite(true)
        .build();

    SavedPrompt existingPrompt = SavedPrompt.builder()
        .id(promptId)
        .name("Old Prompt")
        .description("Old description")
        .prompt("Old prompt content")
        .isFavorite(false)
        .createdAt(LocalDateTime.now().minusDays(1))
        .updatedAt(LocalDateTime.now().minusDays(1))
        .build();

    SavedPrompt updatedPrompt = existingPrompt.toBuilder()
        .name("Updated Prompt")
        .description("Updated description")
        .prompt("Updated prompt content")
        .isFavorite(true)
        .updatedAt(LocalDateTime.now())
        .build();

    when(savedPromptRepository.findById(promptId)).thenReturn(Mono.just(existingPrompt));
    when(savedPromptRepository.save(any(SavedPrompt.class))).thenReturn(Mono.just(updatedPrompt));

    // When & Then
    StepVerifier.create(savedPromptService.updatePrompt(promptId, updateDto))
        .expectNextMatches(result ->
            result.getName().equals("Updated Prompt") &&
                result.getIsFavorite().equals(true))
        .verifyComplete();
  }

  @Test
  void deletePrompt_ValidId_DeletesSuccessfully() {
    // Given
    Long promptId = 1L;
    SavedPrompt prompt = SavedPrompt.builder().id(promptId).build();

    when(savedPromptRepository.findById(promptId)).thenReturn(Mono.just(prompt));
    when(savedPromptRepository.deleteById(promptId)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(savedPromptService.deletePrompt(promptId))
        .verifyComplete();
  }

  @Test
  void estimatePromptCost_ValidPrompt_ReturnsEstimatedCost() {
    // Given
    Long promptId = 1L;
    SavedPrompt prompt = SavedPrompt.builder()
        .id(promptId)
        .prompt("This is a test prompt with some content")
        .systemMessage("System message")
        .build();

    when(savedPromptRepository.findById(promptId)).thenReturn(Mono.just(prompt));

    // When & Then
    StepVerifier.create(savedPromptService.estimatePromptCost(promptId))
        .expectNextMatches(cost -> cost > 0.0)
        .verifyComplete();
  }
}
