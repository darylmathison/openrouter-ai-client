package com.chatgpt.client.controller;

import com.chatgpt.client.dto.ChatRequest;
import com.chatgpt.client.dto.ChatResponse;
import com.chatgpt.client.dto.ChatSummaryDto;
import com.chatgpt.client.dto.SimpleMessageRequest;
import com.chatgpt.client.model.Chat;
import com.chatgpt.client.service.ChatService;
import com.chatgpt.client.service.MarkdownService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

  private final ChatService chatService;
  private final MarkdownService markdownService;

  // Simple message endpoint (for new chats with single message)
  @PostMapping("/simple")
  public Mono<ResponseEntity<ChatResponse>> sendSimpleMessage(
      @Valid @RequestBody SimpleMessageRequest request) {
    return chatService.sendSimpleMessage(null, request)
        .map(ResponseEntity::ok);
  }

  // Complex message endpoint (for full conversation control)
  @PostMapping
  public Mono<ResponseEntity<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
    return chatService.sendMessage(null, request)
        .map(ResponseEntity::ok);
  }

  // Add simple message to existing chat
  @PostMapping("/{chatId}/simple")
  public Mono<ResponseEntity<ChatResponse>> sendSimpleMessageToChat(
      @PathVariable Long chatId,
      @Valid @RequestBody SimpleMessageRequest request) {
    return chatService.sendSimpleMessage(chatId, request)
        .map(ResponseEntity::ok);
  }

  // Add complex messages to existing chat
  @PostMapping("/{chatId}/messages")
  public Mono<ResponseEntity<ChatResponse>> sendMessageToChat(
      @PathVariable Long chatId,
      @Valid @RequestBody ChatRequest request) {
    return chatService.sendMessage(chatId, request)
        .map(ResponseEntity::ok);
  }

  @GetMapping
  public Mono<ResponseEntity<List<ChatSummaryDto>>> getChats(
      @RequestParam(required = false) String search) {
    return chatService.searchChats(search)
        .map(ResponseEntity::ok);
  }

  @GetMapping("/{chatId}")
  public Mono<ResponseEntity<Chat>> getChat(@PathVariable Long chatId) {
    return chatService.getChatById(chatId)
        .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{chatId}")
  public Mono<ResponseEntity<Void>> deleteChat(@PathVariable Long chatId) {
    return chatService.deleteChat(chatId)
        .then(Mono.just(ResponseEntity.noContent().build()));
  }

  @GetMapping("/{chatId}/export")
  public Mono<ResponseEntity<String>> exportChatAsMarkdown(@PathVariable Long chatId) {
    return chatService.exportChatAsMarkdown(chatId)
        .map(markdown -> ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .header("Content-Disposition", "attachment; filename=chat-" + chatId + ".md")
            .body(markdown));
  }

  @PostMapping("/{chatId}/generate-prompt")
  public Mono<ResponseEntity<String>> generatePromptFromChat(@PathVariable Long chatId) {
    return chatService.getChatById(chatId)
        .map(chat -> ResponseEntity.ok(markdownService.generatePromptFromChat(chat)));
  }

  @GetMapping("/cost/monthly")
  public Mono<ResponseEntity<Double>> getMonthlyCost() {
    return chatService.getTotalCostThisMonth()
        .map(ResponseEntity::ok);
  }
}