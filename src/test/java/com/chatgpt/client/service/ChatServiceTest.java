package com.chatgpt.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chatgpt.client.dto.ChatRequest;
import com.chatgpt.client.dto.ChatResponse;
import com.chatgpt.client.dto.TokenUsage;
import com.chatgpt.client.model.Chat;
import com.chatgpt.client.model.Message;
import com.chatgpt.client.repository.ChatRepository;
import com.chatgpt.client.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock
  private ChatRepository chatRepository;

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private AIService openAIService;

  @Mock
  private MarkdownService markdownService;

  private ChatService chatService;

  @BeforeEach
  void setUp() {
    chatService = new ChatService(chatRepository, messageRepository, openAIService,
        markdownService);
  }

  @Test
  void sendMessage_NewChat_CreatesNewChatAndSavesMessages() {
    // Given
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content("Hello, how are you?")
            .role(Message.MessageRole.USER)
            .build()))
        .model("gpt-3.5-turbo")
        .build();

    Chat savedChat = Chat.builder()
        .id(1L)
        .title("Hello, how are you?")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .totalTokens(0L)
        .estimatedCost(0.0)
        .build();

    Message savedUserMessage = Message.builder()
        .id(1L)
        .chatId(1L)
        .content("Hello, how are you?")
        .role(Message.MessageRole.USER)
        .createdAt(LocalDateTime.now())
        .build();

    ChatResponse aiResponse = ChatResponse.builder()
        .content("I'm doing well, thank you!")
        .model("gpt-3.5-turbo")
        .tokenUsage(TokenUsage.builder()
            .promptTokens(10)
            .completionTokens(15)
            .totalTokens(25)
            .build())
        .estimatedCost(0.001)
        .build();

    Message savedAiMessage = Message.builder()
        .id(2L)
        .chatId(1L)
        .content("I'm doing well, thank you!")
        .role(Message.MessageRole.ASSISTANT)
        .tokens(25)
        .createdAt(LocalDateTime.now())
        .build();

    when(chatRepository.save(any(Chat.class))).thenReturn(Mono.just(savedChat));
    when(messageRepository.save(any(Message.class)))
        .thenReturn(Mono.just(savedUserMessage))
        .thenReturn(Mono.just(savedAiMessage));
    when(openAIService.sendChatRequest(any(ChatRequest.class))).thenReturn(Mono.just(aiResponse));
    when(chatRepository.findById(1L)).thenReturn(Mono.just(savedChat));

    // When & Then
    StepVerifier.create(chatService.sendMessage(null, request))
        .expectNextMatches(response ->
            response.getContent().equals("I'm doing well, thank you!") &&
                response.getChatId().equals(1L) &&
                response.getMessageId().equals(2L))
        .verifyComplete();
  }

  @Test
  void sendMessage_ExistingChat_AddsMessageToChat() {
    // Given
    Long chatId = 1L;
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content("How's the weather?")
            .role(Message.MessageRole.USER)
            .build()))
        .build();

    Chat existingChat = Chat.builder()
        .id(chatId)
        .title("Existing Chat")
        .totalTokens(50L)
        .estimatedCost(0.002)
        .build();

    Message savedUserMessage = Message.builder()
        .id(3L)
        .chatId(chatId)
        .content("How's the weather?")
        .role(Message.MessageRole.USER)
        .build();

    ChatResponse aiResponse = ChatResponse.builder()
        .content("It's sunny today!")
        .model("gpt-3.5-turbo")
        .tokenUsage(TokenUsage.builder().totalTokens(20).build())
        .estimatedCost(0.001)
        .build();

    Message savedAiMessage = Message.builder()
        .id(4L)
        .chatId(chatId)
        .content("It's sunny today!")
        .role(Message.MessageRole.ASSISTANT)
        .tokens(20)
        .build();

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(existingChat));
    when(messageRepository.save(any(Message.class)))
        .thenReturn(Mono.just(savedUserMessage))
        .thenReturn(Mono.just(savedAiMessage));
    when(openAIService.sendChatRequest(any(ChatRequest.class))).thenReturn(Mono.just(aiResponse));
    when(chatRepository.save(any(Chat.class))).thenReturn(Mono.just(existingChat));

    // When & Then
    StepVerifier.create(chatService.sendMessage(chatId, request))
        .expectNextMatches(response ->
            response.getContent().equals("It's sunny today!") &&
                response.getChatId().equals(chatId))
        .verifyComplete();
  }

  @Test
  void sendMessage_NullMessages_ThrowsIllegalArgumentException() {
    // Given
    ChatRequest request = ChatRequest.builder()
        .messages(null)  // Null messages list
        .model("gpt-3.5-turbo")
        .build();

    // When & Then
    StepVerifier.create(chatService.sendMessage(null, request))
        .expectErrorMatches(throwable ->
            throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Messages list cannot be null or empty"))
        .verify();
  }

  @Test
  void sendMessage_EmptyMessages_ThrowsIllegalArgumentException() {
    // Given
    ChatRequest request = ChatRequest.builder()
        .messages(Collections.emptyList())  // Empty messages list
        .model("gpt-3.5-turbo")
        .build();

    // When & Then
    try {
      StepVerifier.create(chatService.sendMessage(null, request))
          .expectError()
          .verify();
    } catch (Exception e) {
      // Print the actual exception to see what's happening
      System.out.println("Actual exception: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
      throw e;
    }


  }

  @Test
  void sendMessage_ExistingChat_NullMessages_ThrowsIllegalArgumentException() {
    // Given
    Long chatId = 1L;
    ChatRequest request = ChatRequest.builder()
        .messages(null)  // Null messages list
        .model("gpt-3.5-turbo")
        .build();

    Chat existingChat = Chat.builder()
        .id(chatId)
        .title("Existing Chat")
        .build();

    // When & Then
    StepVerifier.create(chatService.sendMessage(chatId, request))
        .expectErrorMatches(throwable ->
            throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Messages list cannot be null or empty"))
        .verify();
  }

  @Test
  void sendMessage_ExistingChat_EmptyMessages_ThrowsIllegalArgumentException() {
    // Given
    Long chatId = 1L;
    ChatRequest request = ChatRequest.builder()
        .messages(Collections.emptyList())  // Empty messages list
        .model("gpt-3.5-turbo")
        .build();

    Chat existingChat = Chat.builder()
        .id(chatId)
        .title("Existing Chat")
        .build();

    // When & Then
    StepVerifier.create(chatService.sendMessage(chatId, request))
        .expectErrorMatches(throwable ->
            throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Messages list cannot be null or empty"))
        .verify();
  }

  @Test
  void searchChats_WithSearchTerm_ReturnsMatchingChats() {
    // Given
    String searchTerm = "weather";
    Chat matchingChat = Chat.builder()
        .id(1L)
        .title("Weather Discussion")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    Message lastMessage = Message.builder()
        .chatId(1L)
        .content("It's sunny today")
        .role(Message.MessageRole.ASSISTANT)
        .createdAt(LocalDateTime.now())
        .build();

    when(chatRepository.findByTitleContainingIgnoreCaseOrderByUpdatedAtDesc(searchTerm))
        .thenReturn(Flux.just(matchingChat));
    when(messageRepository.countByChatId(1L)).thenReturn(Mono.just(5L));
    when(messageRepository.findTopByChatIdOrderByCreatedAtDesc(1L))
        .thenReturn(Mono.just(lastMessage));

    // When & Then
    StepVerifier.create(chatService.searchChats(searchTerm))
        .expectNextMatches(chats ->
            chats.size() == 1 &&
                chats.get(0).getTitle().equals("Weather Discussion") &&
                chats.get(0).getMessageCount() == 5)
        .verifyComplete();
  }

  @Test
  void searchChats_EmptySearchTerm_ReturnsAllChats() {
    // Given
    Chat chat = Chat.builder()
        .id(1L)
        .title("Test Chat")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(chatRepository.findAllOrderByUpdatedAtDesc()).thenReturn(Flux.just(chat));
    when(messageRepository.countByChatId(1L)).thenReturn(Mono.just(3L));
    when(messageRepository.findTopByChatIdOrderByCreatedAtDesc(1L))
        .thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(chatService.searchChats(""))
        .expectNextMatches(chats ->
            chats.size() == 1 &&
                chats.get(0).getTitle().equals("Test Chat"))
        .verifyComplete();
  }

  @Test
  void getChatById_ValidId_ReturnsChatWithMessages() {
    // Given
    Long chatId = 1L;
    Chat chat = Chat.builder()
        .id(chatId)
        .title("Test Chat")
        .build();

    List<Message> messages = List.of(
        Message.builder()
            .id(1L)
            .chatId(chatId)
            .content("Hello")
            .role(Message.MessageRole.USER)
            .build(),
        Message.builder()
            .id(2L)
            .chatId(chatId)
            .content("Hi there!")
            .role(Message.MessageRole.ASSISTANT)
            .build()
    );

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(chat));
    when(messageRepository.findByChatIdOrderByCreatedAtAsc(chatId))
        .thenReturn(Flux.fromIterable(messages));

    // When & Then
    StepVerifier.create(chatService.getChatById(chatId))
        .expectNextMatches(result ->
            result.getId().equals(chatId) &&
                result.getMessages().size() == 2)
        .verifyComplete();
  }

  @Test
  void deleteChat_ValidId_DeletesChatAndMessages() {
    // Given
    Long chatId = 1L;
    Chat chat = Chat.builder().id(chatId).build();

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(chat));
    when(messageRepository.deleteByChatId(chatId)).thenReturn(Mono.empty());
    when(chatRepository.deleteById(chatId)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(chatService.deleteChat(chatId))
        .verifyComplete();
  }

  @Test
  void exportChatAsMarkdown_ValidId_ReturnsMarkdown() {
    // Given
    Long chatId = 1L;
    Chat chat = Chat.builder()
        .id(chatId)
        .title("Test Chat")
        .build();

    String expectedMarkdown = "# Test Chat\n\n**User:** Hello\n\n**Assistant:** Hi there!";

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(chat));
    when(messageRepository.findByChatIdOrderByCreatedAtAsc(chatId))
        .thenReturn(Flux.empty());
    when(markdownService.convertChatToMarkdown(any(Chat.class)))
        .thenReturn(expectedMarkdown);

    // When & Then
    StepVerifier.create(chatService.exportChatAsMarkdown(chatId))
        .expectNext(expectedMarkdown)
        .verifyComplete();
  }

  @Test
  void getTotalCostThisMonth_ReturnsCalculatedCost() {
    // Given
    Double expectedCost = 15.50;
    when(chatRepository.getTotalCostSince(any(LocalDateTime.class)))
        .thenReturn(Mono.just(expectedCost));

    // When & Then
    StepVerifier.create(chatService.getTotalCostThisMonth())
        .expectNext(expectedCost)
        .verifyComplete();
  }

  @Test
  void sendMessage_ChatNotFound_ThrowsException() {
    // Given
    Long nonExistentChatId = 999L;
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content("Hello")
            .role(Message.MessageRole.USER)
            .build()))
        .build();

    when(chatRepository.findById(nonExistentChatId)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(chatService.sendMessage(nonExistentChatId, request))
        .expectError(RuntimeException.class)
        .verify();
  }
}
