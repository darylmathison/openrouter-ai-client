package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MCPServiceTest {

    @Mock
    private ExternalToolRepository externalToolRepository;

    @Mock
    private MCPToolExecutor mcpToolExecutor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonNode jsonNode;

    private MCPService mcpService;

    @BeforeEach
    void setUp() {
        mcpService = new MCPService(externalToolRepository, mcpToolExecutor, objectMapper);
    }

    @Test
    void parseAndProcessMessage_NoToolCall_ReturnsOriginalMessage() {
        // Given
        String message = "Hello, how are you?";

        // When & Then
        StepVerifier.create(mcpService.parseAndProcessMessage(message))
            .expectNext(message)
            .verifyComplete();

        // Verify no interactions with repository or executor
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(mcpToolExecutor, never()).executeToolRequest(any(ExternalTool.class), anyMap());
    }

    @Test
    void parseAndProcessMessage_WithToolCall_ExecutesTool() {
        // Given
        String message = "@{{Weather}} What's the weather like in New York?";
        String toolName = "Weather";
        String toolInput = "What's the weather like in New York?";
        String toolOutput = "The weather in New York is sunny, 72°F";
        String formattedOutput = "{\"tool_name\":\"Weather\",\"result\":\"The weather in New York is sunny, 72°F\"}";

        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name(toolName)
            .isActive(true)
            .toolType("API")
            .build();

        when(externalToolRepository.findByNameIgnoreCase(toolName)).thenReturn(Mono.just(tool));
        when(mcpToolExecutor.executeToolRequest(eq(tool), any(Map.class))).thenReturn(Mono.just(toolOutput));
        
        try {
            when(objectMapper.readTree(toolOutput)).thenReturn(jsonNode);
            when(objectMapper.writeValueAsString(any())).thenReturn(formattedOutput);
        } catch (Exception e) {
            // Ignore exception in test setup
        }

        // When & Then
        StepVerifier.create(mcpService.parseAndProcessMessage(message))
            .expectNext(formattedOutput)
            .verifyComplete();

        // Verify interactions
        verify(externalToolRepository).findByNameIgnoreCase(toolName);
        verify(mcpToolExecutor).executeToolRequest(eq(tool), any(Map.class));
    }

    @Test
    void parseAndProcessMessage_ToolNotFound_ReturnsError() {
        // Given
        String message = "@{{NonExistentTool}} Do something";
        String toolName = "NonExistentTool";

        when(externalToolRepository.findByNameIgnoreCase(toolName)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(mcpService.parseAndProcessMessage(message))
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("External tool not found"))
            .verify();

        // Verify interactions
        verify(externalToolRepository).findByNameIgnoreCase(toolName);
        verify(mcpToolExecutor, never()).executeToolRequest(any(ExternalTool.class), anyMap());
    }

    @Test
    void parseAndProcessMessage_EmptyMessage_ReturnsEmptyString() {
        // Given
        String message = "";

        // When & Then
        StepVerifier.create(mcpService.parseAndProcessMessage(message))
            .expectNext("")
            .verifyComplete();

        // Verify no interactions
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(mcpToolExecutor, never()).executeToolRequest(any(ExternalTool.class), anyMap());
    }

    @Test
    void parseAndProcessMessage_NullMessage_ReturnsEmptyString() {
        // When & Then
        StepVerifier.create(mcpService.parseAndProcessMessage(null))
            .expectNext("")
            .verifyComplete();

        // Verify no interactions
        verify(externalToolRepository, never()).findByNameIgnoreCase(anyString());
        verify(mcpToolExecutor, never()).executeToolRequest(any(ExternalTool.class), anyMap());
    }

    @Test
    void executeMCPTool_SuccessfulExecution_ReturnsFormattedResponse() {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("Weather")
            .toolType("API")
            .isActive(true)
            .build();
        
        String input = "What's the weather like in New York?";
        String toolOutput = "The weather in New York is sunny, 72°F";
        String formattedOutput = "{\"tool_name\":\"Weather\",\"result\":\"The weather in New York is sunny, 72°F\"}";

        when(mcpToolExecutor.executeToolRequest(eq(tool), any(Map.class))).thenReturn(Mono.just(toolOutput));
        
        try {
            when(objectMapper.readTree(toolOutput)).thenReturn(jsonNode);
            when(objectMapper.writeValueAsString(any())).thenReturn(formattedOutput);
        } catch (Exception e) {
            // Ignore exception in test setup
        }

        // When & Then
        StepVerifier.create(mcpService.executeMCPTool(tool, input))
            .expectNext(formattedOutput)
            .verifyComplete();

        // Verify interactions
        verify(mcpToolExecutor).executeToolRequest(eq(tool), any(Map.class));
    }

    @Test
    void executeMCPTool_ExecutionFails_ReturnsError() {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("Weather")
            .toolType("API")
            .isActive(true)
            .build();
        
        String input = "What's the weather like in New York?";

        when(mcpToolExecutor.executeToolRequest(eq(tool), any(Map.class)))
            .thenReturn(Mono.error(new RuntimeException("Tool execution failed")));

        // When & Then
        StepVerifier.create(mcpService.executeMCPTool(tool, input))
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("Tool execution failed"))
            .verify();

        // Verify interactions
        verify(mcpToolExecutor).executeToolRequest(eq(tool), any(Map.class));
    }

    @Test
    void createMCPWrapperForRESTServer_SuccessfulCreation_ReturnsWrapper() {
        // Given
        String restServerUrl = "https://api.example.com";
        String restServerName = "ExampleAPI";

        ExternalTool expectedWrapper = ExternalTool.builder()
            .id(1L)
            .name(restServerName)
            .description("MCP wrapper for REST server: " + restServerUrl)
            .endpointUrl(restServerUrl)
            .httpMethod(ExternalTool.HttpMethod.POST)
            .authType(ExternalTool.AuthType.NONE)
            .requestTemplate("{\"query\": \"{{input}}\", \"mcp_enabled\": true}")
            .responseMapping("{\"extract\": \"/result\"}")
            .isActive(true)
            .toolType("MCP_REST_WRAPPER")
            .build();

        when(externalToolRepository.save(any(ExternalTool.class))).thenReturn(Mono.just(expectedWrapper));

        // When & Then
        StepVerifier.create(mcpService.createMCPWrapperForRESTServer(restServerUrl, restServerName))
            .expectNextMatches(wrapper -> 
                wrapper.getName().equals(restServerName) &&
                wrapper.getEndpointUrl().equals(restServerUrl) &&
                wrapper.getToolType().equals("MCP_REST_WRAPPER"))
            .verifyComplete();

        // Verify interactions
        verify(externalToolRepository).save(any(ExternalTool.class));
    }
}