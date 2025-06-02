package com.darylmathison.chat.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.darylmathison.chat.client.dto.ChatRequest;
import com.darylmathison.chat.client.dto.ChatResponse;
import com.darylmathison.chat.client.dto.TokenUsage;
import com.darylmathison.chat.client.model.Chat;
import com.darylmathison.chat.client.model.Message;
import com.darylmathison.chat.client.repository.ChatRepository;
import com.darylmathison.chat.client.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(ChatServiceTest.class);

  @Mock
  private ChatRepository chatRepository;

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private AIService openAIService;

  @Mock
  private MarkdownService markdownService;

  @Mock
  private MessageParserService messageParserService;

  private ChatService chatService;

  @BeforeEach
  void setUp() {
    chatService = new ChatService(chatRepository, messageRepository, openAIService,
        markdownService, messageParserService);
  }

  @Test
  void sendMessage_NewChat_CreatesNewChatAndSavesMessages() {
    // Given
    String userContent = "Hello, how are you?";
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content(userContent)
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
        .content(userContent)
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

    // Mock the message parser to return the original content (no tool call)
    when(messageParserService.parseAndProcessMessage(userContent))
        .thenReturn(Mono.just(userContent));

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
    String userContent = "How's the weather?";
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content(userContent)
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
        .content(userContent)
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

    // Mock the message parser to return the original content (no tool call)
    when(messageParserService.parseAndProcessMessage(userContent))
        .thenReturn(Mono.just(userContent));

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

    // No need to mock messageParserService as the method should return early with an error

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

    // No need to mock messageParserService as the method should return early with an error

    // When & Then
    try {
      StepVerifier.create(chatService.sendMessage(null, request))
          .expectError()
          .verify();
    } catch (Exception e) {
      // Log the actual exception to see what's happening
      logger.debug("Actual exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
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


    // No need to mock messageParserService as the method should return early with an error

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

    // No need to mock messageParserService as the method should return early with an error

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
                chats.getFirst().getTitle().equals("Weather Discussion") &&
                chats.getFirst().getMessageCount() == 5)
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
                chats.getFirst().getTitle().equals("Test Chat"))
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
    String userContent = "Hello";
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content(userContent)
            .role(Message.MessageRole.USER)
            .build()))
        .build();

    // Note: We don't need to mock messageParserService here because the test is
    // specifically checking for a chat not found error, which happens before
    // the message parser would be used in a real scenario

    when(chatRepository.findById(nonExistentChatId)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(chatService.sendMessage(nonExistentChatId, request))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void sendMessage_WithToolCall_ProcessesToolCallAndSendsModifiedRequest() {
    // Given
    Long chatId = 1L;
    String originalContent = "@{{Weather}} What's the weather like in New York?";
    String processedContent = "The weather in New York is sunny, 72°F";

    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content(originalContent)
            .role(Message.MessageRole.USER)
            .build()))
        .model("gpt-3.5-turbo")
        .build();

    Chat existingChat = Chat.builder()
        .id(chatId)
        .title("Weather Chat")
        .build();

    Message savedUserMessage = Message.builder()
        .id(1L)
        .chatId(chatId)
        .content(originalContent)
        .role(Message.MessageRole.USER)
        .createdAt(LocalDateTime.now())
        .build();

    ChatResponse aiResponse = ChatResponse.builder()
        .content("I'll help you with that weather information!")
        .model("gpt-3.5-turbo")
        .tokenUsage(TokenUsage.builder().totalTokens(25).build())
        .estimatedCost(0.001)
        .build();

    Message savedAiMessage = Message.builder()
        .id(2L)
        .chatId(chatId)
        .content("I'll help you with that weather information!")
        .role(Message.MessageRole.ASSISTANT)
        .tokens(25)
        .createdAt(LocalDateTime.now())
        .build();

    // Mock the message parser to return the processed content
    when(messageParserService.parseAndProcessMessage(originalContent))
        .thenReturn(Mono.just(processedContent));

    // Note: We don't need to mock messageParserService.parseAndProcessMessage(processedContent)
    // because in the actual flow, the processed content is used directly without being
    // processed again

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(existingChat));
    when(messageRepository.save(any(Message.class)))
        .thenReturn(Mono.just(savedUserMessage))
        .thenReturn(Mono.just(savedAiMessage));
    when(openAIService.sendChatRequest(any(ChatRequest.class))).thenReturn(Mono.just(aiResponse));
    when(chatRepository.save(any(Chat.class))).thenReturn(Mono.just(existingChat));

    // When & Then
    StepVerifier.create(chatService.sendMessage(chatId, request))
        .expectNextMatches(response ->
            response.getContent().equals("I'll help you with that weather information!") &&
            response.getChatId().equals(chatId))
        .verifyComplete();
  }

  @Test
  void sendSimpleMessage_WithToolCall_ProcessesToolCallAndSendsModifiedRequest() {
    // Given
    Long chatId = 1L;
    String originalContent = "@{{Weather}} What's the weather like in New York?";
    String processedContent = "The weather in New York is sunny, 72°F";

    com.darylmathison.chat.client.dto.SimpleMessageRequest request = 
        com.darylmathison.chat.client.dto.SimpleMessageRequest.builder()
            .message(originalContent)
            .model("gpt-3.5-turbo")
            .build();

    Chat existingChat = Chat.builder()
        .id(chatId)
        .title("Weather Chat")
        .build();

    Message savedUserMessage = Message.builder()
        .id(1L)
        .chatId(chatId)
        .content(originalContent)
        .role(Message.MessageRole.USER)
        .createdAt(LocalDateTime.now())
        .build();

    ChatResponse aiResponse = ChatResponse.builder()
        .content("I'll help you with that weather information!")
        .model("gpt-3.5-turbo")
        .tokenUsage(TokenUsage.builder().totalTokens(25).build())
        .estimatedCost(0.001)
        .build();

    Message savedAiMessage = Message.builder()
        .id(2L)
        .chatId(chatId)
        .content("I'll help you with that weather information!")
        .role(Message.MessageRole.ASSISTANT)
        .tokens(25)
        .createdAt(LocalDateTime.now())
        .build();

    // Mock the message parser to return the processed content
    when(messageParserService.parseAndProcessMessage(originalContent))
        .thenReturn(Mono.just(processedContent));

    // Unlike in sendMessage, in sendSimpleMessage the processed content is processed again,
    // so we need to mock this call as well
    when(messageParserService.parseAndProcessMessage(processedContent))
        .thenReturn(Mono.just(processedContent));

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(existingChat));
    when(messageRepository.save(any(Message.class)))
        .thenReturn(Mono.just(savedUserMessage))
        .thenReturn(Mono.just(savedAiMessage));
    when(openAIService.sendChatRequest(any(ChatRequest.class))).thenReturn(Mono.just(aiResponse));
    when(chatRepository.save(any(Chat.class))).thenReturn(Mono.just(existingChat));

    // When & Then
    StepVerifier.create(chatService.sendSimpleMessage(chatId, request))
        .expectNextMatches(response ->
            response.getContent().equals("I'll help you with that weather information!") &&
            response.getChatId().equals(chatId))
        .verifyComplete();
  }

  @Test
  void sendMessage_ToolCallProcessingFails_PropagatesError() {
    // Given
    Long chatId = 1L;
    String originalContent = "@{{Weather}} What's the weather like in New York?";

    ChatRequest request = ChatRequest.builder()
        .messages(List.of(Message.builder()
            .content(originalContent)
            .role(Message.MessageRole.USER)
            .build()))
        .model("gpt-3.5-turbo")
        .build();

    Chat existingChat = Chat.builder()
        .id(chatId)
        .title("Weather Chat")
        .build();

    // Mock the message parser to return an error
    when(messageParserService.parseAndProcessMessage(originalContent))
        .thenReturn(Mono.error(new RuntimeException("Tool execution failed")));

    when(chatRepository.findById(chatId)).thenReturn(Mono.just(existingChat));

    // When & Then
    StepVerifier.create(chatService.sendMessage(chatId, request))
        .expectErrorMatches(throwable ->
            throwable instanceof RuntimeException &&
            throwable.getMessage().contains("Tool execution failed"))
        .verify();
  }
}
