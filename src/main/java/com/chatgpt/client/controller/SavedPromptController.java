package com.chatgpt.client.controller;

import com.chatgpt.client.dto.ChatResponse;
import com.chatgpt.client.dto.SavedPromptDto;
import com.chatgpt.client.service.SavedPromptService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SavedPromptController {

  private final SavedPromptService savedPromptService;

  @PostMapping
  public Mono<ResponseEntity<SavedPromptDto>> savePrompt(
      @Valid @RequestBody SavedPromptDto promptDto) {
    return savedPromptService.savePrompt(promptDto)
        .map(ResponseEntity::ok);
  }

  @GetMapping
  public Mono<ResponseEntity<List<SavedPromptDto>>> getAllPrompts(
      @RequestParam(required = false) String search) {
    return (search != null ?
        savedPromptService.searchPrompts(search) :
        savedPromptService.getAllPrompts())
        .map(ResponseEntity::ok);
  }

  @PostMapping("/{promptId}/execute")
  public Mono<ResponseEntity<ChatResponse>> executePrompt(@PathVariable Long promptId) {
    return savedPromptService.executePrompt(promptId)
        .map(ResponseEntity::ok);
  }

  @PutMapping("/{promptId}")
  public Mono<ResponseEntity<SavedPromptDto>> updatePrompt(
      @PathVariable Long promptId,
      @Valid @RequestBody SavedPromptDto promptDto) {
    return savedPromptService.updatePrompt(promptId, promptDto)
        .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{promptId}")
  public Mono<ResponseEntity<Void>> deletePrompt(@PathVariable Long promptId) {
    return savedPromptService.deletePrompt(promptId)
        .then(Mono.just(ResponseEntity.noContent().build()));
  }

  @GetMapping("/{promptId}/estimate-cost")
  public Mono<ResponseEntity<Double>> estimatePromptCost(@PathVariable Long promptId) {
    return savedPromptService.estimatePromptCost(promptId)
        .map(ResponseEntity::ok);
  }
}