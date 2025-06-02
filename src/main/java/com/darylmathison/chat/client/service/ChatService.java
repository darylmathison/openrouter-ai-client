package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.dto.ChatRequest;
import com.darylmathison.chat.client.dto.ChatResponse;
import com.darylmathison.chat.client.dto.ChatSummaryDto;
import com.darylmathison.chat.client.dto.SimpleMessageRequest;
import com.darylmathison.chat.client.model.Chat;
import com.darylmathison.chat.client.model.Message;
import com.darylmathison.chat.client.model.Message.MessageRole;
import com.darylmathison.chat.client.repository.ChatRepository;
import com.darylmathison.chat.client.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;
  private final AIService openAIService;
  private final MarkdownService markdownService;
  private final MessageParserService messageParserService;

  public Mono<ChatResponse> sendMessage(Long chatId, ChatRequest request) {
    if (request.getMessages() == null || request.getMessages().isEmpty()) {
      return Mono.error(new IllegalArgumentException("Messages list cannot be null or empty"));
    }

    if (chatId == null) {
      // Create new chat
      return createNewChatWithMessage(request);
    } else {
      // Add message to existing chat
      return addMessageToExistingChat(chatId, request);
    }
  }

  private Mono<ChatResponse> createNewChatWithMessage(ChatRequest request) {
    // Create new chat
    Chat newChat = Chat.builder()
        .title(generateChatTitle(request.getMessages().getFirst().getContent()))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .totalTokens(0L)
        .estimatedCost(0.0)
        .build();

    return chatRepository.save(newChat)
        .flatMap(savedChat -> processMessageAndGetResponse(savedChat.getId(), request));
  }

  private Mono<ChatResponse> addMessageToExistingChat(Long chatId, ChatRequest request) {
    return chatRepository.findById(chatId)
        .switchIfEmpty(Mono.error(new RuntimeException("Chat not found with id: " + chatId)))
        .flatMap(chat -> processMessageAndGetResponse(chatId, request));
  }

  private Mono<ChatResponse> processMessageAndGetResponse(Long chatId, ChatRequest request) {
    String userContent = request.getMessages().getFirst().getContent();

    // Parse and process the message to detect and execute external tool calls
    return messageParserService.parseAndProcessMessage(userContent)
        .flatMap(processedContent -> {
            // Save user message with original content
            Message userMessage = Message.builder()
                .chatId(chatId)
                .content(userContent)
                .role(MessageRole.USER)
                .createdAt(LocalDateTime.now())
                .build();

            return messageRepository.save(userMessage)
                .flatMap(savedUserMessage -> {
                    // If the content was processed by a tool, create a modified request
                    ChatRequest modifiedRequest = request;
                    if (!processedContent.equals(userContent)) {
                        // Create a copy of the first message with the processed content
                        Message processedMessage = Message.builder()
                            .content(processedContent)
                            .role(request.getMessages().getFirst().getRole())
                            .build();

                        // Create a new request with the processed message
                        modifiedRequest = ChatRequest.builder()
                            .messages(List.of(processedMessage))
                            .model(request.getModel())
                            .maxTokens(request.getMaxTokens())
                            .temperature(request.getTemperature())
                            .systemMessage(request.getSystemMessage())
                            .attachmentIds(request.getAttachmentIds())
                            .externalToolIds(request.getExternalToolIds())
                            .build();

                        // Log that the message was processed by a tool
                        log.info("Message processed by external tool: original='{}', processed='{}'", 
                            userContent, processedContent);
                    }

                    // Get AI response
                    return openAIService.sendChatRequest(modifiedRequest)
                        .flatMap(aiResponse -> {
                            // Save AI response message
                            Message aiMessage = Message.builder()
                                .chatId(chatId)
                                .content(aiResponse.getContent())
                                .role(MessageRole.ASSISTANT)
                                .createdAt(LocalDateTime.now())
                                .tokens(aiResponse.getTokenUsage() != null ? aiResponse.getTokenUsage()
                                    .getTotalTokens() : 0)
                                .build();

                            return messageRepository.save(aiMessage)
                                .flatMap(savedAiMessage -> {
                                    // Update chat with token usage and cost
                                    return updateChatStats(chatId, aiResponse)
                                        .then(Mono.just(ChatResponse.builder()
                                            .content(aiResponse.getContent())
                                            .model(aiResponse.getModel())
                                            .temperature(request.getTemperature())
                                            .tokenUsage(aiResponse.getTokenUsage())
                                            .estimatedCost(aiResponse.getEstimatedCost())
                                            .chatId(chatId)
                                            .messageId(savedAiMessage.getId())
                                            .build()));
                                });
                        });
                });
        })
        .doOnSuccess(response -> log.info("Processed message for chat {}", chatId))
        .doOnError(error -> log.error("Error processing message for chat {}: {}", chatId,
            error.getMessage()));
  }

  private Mono<Chat> updateChatStats(Long chatId, ChatResponse aiResponse) {
    return chatRepository.findById(chatId)
        .flatMap(chat -> {
          if (aiResponse.getTokenUsage() != null) {
            chat.setTotalTokens((chat.getTotalTokens() != null ? chat.getTotalTokens() : 0L) +
                aiResponse.getTokenUsage().getTotalTokens());
          }
          if (aiResponse.getEstimatedCost() != null) {
            chat.setEstimatedCost(
                (chat.getEstimatedCost() != null ? chat.getEstimatedCost() : 0.0) +
                    aiResponse.getEstimatedCost());
          }
          chat.setModelUsed(aiResponse.getModel());
          chat.setUpdatedAt(LocalDateTime.now());
          return chatRepository.save(chat);
        });
  }

  public Mono<List<ChatSummaryDto>> searchChats(String search) {
    Flux<Chat> chatFlux;

    if (search == null || search.trim().isEmpty()) {
      chatFlux = chatRepository.findAllOrderByUpdatedAtDesc(); // Fixed method name
    } else {
      chatFlux = chatRepository.findByTitleContainingIgnoreCaseOrderByUpdatedAtDesc(search.trim());
    }

    return chatFlux
        .flatMap(this::convertToChatSummary)
        .collectList()
        .doOnSuccess(chats -> log.info("Found {} chats for search: {}", chats.size(), search));
  }


  public Mono<Chat> getChatById(Long chatId) {
    return chatRepository.findById(chatId)
        .switchIfEmpty(Mono.error(new RuntimeException("Chat not found with id: " + chatId)))
        .flatMap(this::loadMessagesForChat)
        .doOnSuccess(chat -> log.info("Retrieved chat {} with {} messages", chatId,
            chat.getMessages() != null ? chat.getMessages().size() : 0));
  }

  public Mono<Void> deleteChat(Long chatId) {
    return chatRepository.findById(chatId)
        .switchIfEmpty(Mono.error(new RuntimeException("Chat not found with id: " + chatId)))
        .flatMap(chat -> {
          // Delete all messages first (cascade should handle this, but being explicit)
          return messageRepository.deleteByChatId(chatId)
              .then(chatRepository.deleteById(chatId));
        })
        .doOnSuccess(v -> log.info("Deleted chat {}", chatId))
        .doOnError(error -> log.error("Error deleting chat {}: {}", chatId, error.getMessage()));
  }

  public Mono<String> exportChatAsMarkdown(Long chatId) {
    return getChatById(chatId)
        .map(markdownService::convertChatToMarkdown)
        .doOnSuccess(markdown -> log.info("Exported chat {} as markdown", chatId))
        .doOnError(error -> log.error("Error exporting chat {} as markdown: {}", chatId,
            error.getMessage()));
  }

  public Mono<Double> getTotalCostThisMonth() {
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0)
        .withSecond(0);

    // Use the existing repository method instead of the one that doesn't exist
    return chatRepository.getTotalCostSince(startOfMonth)
        .defaultIfEmpty(0.0)
        .doOnSuccess(cost -> log.info("Total cost this month: ${}", cost))
        .doOnError(error -> log.error("Error calculating monthly cost: {}", error.getMessage()));
  }

// Add this method to your ChatService class

  public Mono<ChatResponse> sendSimpleMessage(Long chatId, SimpleMessageRequest request) {
    if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
      return Mono.error(new IllegalArgumentException("Message cannot be null or empty"));
    }

    String userContent = request.getMessage().trim();

    // Parse and process the message to detect and execute external tool calls
    return messageParserService.parseAndProcessMessage(userContent)
        .flatMap(processedContent -> {
            // Convert SimpleMessageRequest to ChatRequest
            Message userMessage = Message.builder()
                .content(userContent)
                .role(Message.MessageRole.USER)
                .build();

            // If the content was processed by a tool, use the processed content
            if (!processedContent.equals(userContent)) {
                log.info("Simple message processed by external tool: original='{}', processed='{}'", 
                    userContent, processedContent);

                // Create a message with the processed content
                Message processedMessage = Message.builder()
                    .content(processedContent)
                    .role(Message.MessageRole.USER)
                    .build();

                ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(processedMessage))
                    .model(request.getModel())
                    .maxTokens(request.getMaxTokens())
                    .temperature(request.getTemperature())
                    .systemMessage(request.getSystemMessage())
                    .attachmentIds(request.getAttachmentIds())
                    .externalToolIds(request.getExternalToolIds())
                    .build();

                // Delegate to existing method
                return sendMessage(chatId, chatRequest);
            } else {
                // Use the original content
                ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(userMessage))
                    .model(request.getModel())
                    .maxTokens(request.getMaxTokens())
                    .temperature(request.getTemperature())
                    .systemMessage(request.getSystemMessage())
                    .attachmentIds(request.getAttachmentIds())
                    .externalToolIds(request.getExternalToolIds())
                    .build();

                // Delegate to existing method
                return sendMessage(chatId, chatRequest);
            }
        });
  }

  private Mono<Chat> loadMessagesForChat(Chat chat) {
    return messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId())
        .collectList()
        .map(messages -> {
          chat.setMessages(messages);
          return chat;
        });
  }

  private Mono<ChatSummaryDto> convertToChatSummary(Chat chat) {
    return messageRepository.countByChatId(chat.getId())
        .flatMap(messageCount ->
            messageRepository.findTopByChatIdOrderByCreatedAtDesc(chat.getId())
                .map(Message::getContent)
                .defaultIfEmpty("")
                .map(lastMessageContent -> ChatSummaryDto.builder()
                    .id(chat.getId())
                    .title(chat.getTitle())
                    .createdAt(chat.getCreatedAt())
                    .updatedAt(chat.getUpdatedAt())
                    .messageCount(messageCount.intValue())
                    .lastMessagePreview(truncateMessage(lastMessageContent))
                    .estimatedCost(chat.getEstimatedCost())
                    .modelUsed(chat.getModelUsed())
                    .build())
        );
  }

  private String generateChatTitle(String firstMessage) {
    // Generate a title from the first message (truncate and clean up)
    if (firstMessage == null || firstMessage.trim().isEmpty()) {
      return "New Chat";
    }

    String title = firstMessage.trim();
    if (title.length() > 50) {
      title = title.substring(0, 47) + "...";
    }

    return title;
  }

  private String truncateMessage(String message) {
    if (message == null || message.trim().isEmpty()) {
      return "";
    }

    String truncated = message.trim();
    if (truncated.length() > 100) {
      truncated = truncated.substring(0, 97) + "...";
    }

    return truncated;
  }
}
