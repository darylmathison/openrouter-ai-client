package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageParserServiceTest {

    @Mock
    private ExternalToolRepository externalToolRepository;

    @Mock
    private ExternalToolService externalToolService;

    @Mock
    private MCPService mcpService;

    private MessageParserService messageParserService;

    @BeforeEach
    void setUp() {
        messageParserService = new MessageParserService(externalToolRepository, externalToolService);
    }

    @Test
    void parseAndProcessMessage_NoToolCall_ReturnsOriginalMessage() {
        // Given
        String message = "Hello, how are you?";

        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectNext(message)
            .verifyComplete();

        // Verify no interactions with repository or service
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(externalToolService, never()).executeTool(anyLong(), anyMap());
    }

    @Test
    void parseAndProcessMessage_WithToolCall_ExecutesTool() {
        // Given
        String message = "@{{Weather}} What's the weather like in New York?";
        String toolName = "Weather";
        String toolInput = "What's the weather like in New York?";
        String toolOutput = "The weather in New York is sunny, 72°F";

        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name(toolName)
            .isActive(true)
            .build();

        when(externalToolRepository.findByNameIgnoreCase(toolName)).thenReturn(Mono.just(tool));
        when(externalToolService.executeTool(anyLong(), any(Map.class))).thenReturn(Mono.just(toolOutput));

        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectNext(toolOutput)
            .verifyComplete();

        // Verify interactions
        verify(externalToolRepository).findByNameIgnoreCase(toolName);
        verify(externalToolService).executeTool(anyLong(), any(Map.class));
    }

    @Test
    void parseAndProcessMessage_ToolNotFound_ReturnsError() {
        // Given
        String message = "@{{NonExistentTool}} Do something";
        String toolName = "NonExistentTool";

        when(externalToolRepository.findByNameIgnoreCase(toolName)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("External tool not found"))
            .verify();

        // Verify interactions
        verify(externalToolRepository).findByNameIgnoreCase(toolName);
        verify(externalToolService, never()).executeTool(anyLong(), any(Map.class));
    }

    @Test
    void parseAndProcessMessage_ToolExecutionFails_ReturnsError() {
        // Given
        String message = "@{{Weather}} What's the weather like in New York?";
        String toolName = "Weather";

        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name(toolName)
            .isActive(true)
            .build();

        when(externalToolRepository.findByNameIgnoreCase(toolName)).thenReturn(Mono.just(tool));
        when(externalToolService.executeTool(anyLong(), any(Map.class)))
            .thenReturn(Mono.error(new RuntimeException("Tool execution failed")));

        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("Tool execution failed"))
            .verify();

        // Verify interactions
        verify(externalToolRepository).findByNameIgnoreCase(toolName);
        verify(externalToolService).executeTool(anyLong(), any(Map.class));
    }

    @Test
    void parseAndProcessMessage_EmptyMessage_ReturnsEmptyString() {
        // Given
        String message = "";

        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectNext("")
            .verifyComplete();

        // Verify no interactions
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(externalToolService, never()).executeTool(anyLong(), anyMap());
    }

    @Test
    void parseAndProcessMessage_NullMessage_ReturnsEmptyString() {
        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(null))
            .expectNext("")
            .verifyComplete();

        // Verify no interactions
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(externalToolService, never()).executeTool(anyLong(), anyMap());
    }

    @Test
    void setMcpService_ShouldSetMcpService() {
        // Given
        String message = "@{{Weather}} What's the weather like in New York?";
        String processedMessage = "The weather in New York is sunny, 72°F";

        when(mcpService.parseAndProcessMessage(message)).thenReturn(Mono.just(processedMessage));

        // When
        messageParserService.setMcpService(mcpService);

        // Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectNext(processedMessage)
            .verifyComplete();

        // Verify MCPService was used
        verify(mcpService).parseAndProcessMessage(message);

        // Verify no interactions with repository or service
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(externalToolService, never()).executeTool(anyLong(), anyMap());
    }

    @Test
    void parseAndProcessMessage_WithMcpService_DelegatesToMcpService() {
        // Given
        String message = "@{{Weather}} What's the weather like in New York?";
        String processedMessage = "The weather in New York is sunny, 72°F";

        // Set MCPService
        messageParserService.setMcpService(mcpService);

        when(mcpService.parseAndProcessMessage(message)).thenReturn(Mono.just(processedMessage));

        // When & Then
        StepVerifier.create(messageParserService.parseAndProcessMessage(message))
            .expectNext(processedMessage)
            .verifyComplete();

        // Verify MCPService was used
        verify(mcpService).parseAndProcessMessage(message);

        // Verify no interactions with repository or service
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(externalToolService, never()).executeTool(anyLong(), anyMap());
    }
}
