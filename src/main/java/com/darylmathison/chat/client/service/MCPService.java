package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model, Chat, Plugin (MCP) Service for handling external tool interactions through chat.
 * This service provides a standardized way to interact with external tools through the chat interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MCPService {

    private final ExternalToolRepository externalToolRepository;
    private final MCPToolExecutor mcpToolExecutor;
    private final ObjectMapper objectMapper;

    // Pattern to match MCP tool call format: @{{name}} input
    private static final Pattern MCP_TOOL_CALL_PATTERN = Pattern.compile("@\\{\\{([^}]+)\\}\\}\\s*(.*)");

    /**
     * Parse a message and detect MCP tool calls.
     * If a tool call is detected, execute the tool and return the result.
     * Otherwise, return the original message.
     *
     * @param message The message to parse
     * @return A Mono containing the processed message
     */
    public Mono<String> parseAndProcessMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Mono.just("");
        }

        Matcher matcher = MCP_TOOL_CALL_PATTERN.matcher(message);
        if (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String toolInput = matcher.group(2).trim();

            log.info("Detected MCP tool call: tool={}, input={}", toolName, toolInput);

            return externalToolRepository.findByNameIgnoreCase(toolName)
                .switchIfEmpty(Mono.error(new RuntimeException("External tool not found: " + toolName)))
                .flatMap(tool -> executeMCPTool(tool, toolInput));
        }

        return Mono.just(message);
    }

    /**
     * Execute an external tool using the MCP protocol.
     * This method wraps the external tool execution with MCP-specific processing.
     *
     * @param tool The external tool to execute
     * @param input The input for the tool
     * @return A Mono containing the processed result
     */
    public Mono<String> executeMCPTool(ExternalTool tool, String input) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", input);

        // Add MCP-specific parameters
        parameters.put("mcp_enabled", true);
        parameters.put("mcp_context_expansion", true);

        return mcpToolExecutor.executeToolRequest(tool, parameters)
            .map(result -> formatMCPResponse(tool, result, input))
            .doOnSuccess(result -> log.info("MCP tool execution successful: {}", result))
            .doOnError(error -> log.error("Error executing MCP tool: {}", error.getMessage()));
    }

    /**
     * Format the response from an external tool according to the MCP protocol.
     * This adds metadata and context information to the response.
     *
     * @param tool The external tool that was executed
     * @param result The raw result from the tool
     * @param input The original input to the tool
     * @return The formatted MCP response
     */
    private String formatMCPResponse(ExternalTool tool, String result, String input) {
        try {
            // Try to parse the result as JSON
            JsonNode resultNode = objectMapper.readTree(result);

            // Create MCP response format
            Map<String, Object> mcpResponse = new HashMap<>();
            mcpResponse.put("tool_name", tool.getName());
            mcpResponse.put("tool_type", tool.getToolType());
            mcpResponse.put("input", input);
            mcpResponse.put("result", resultNode);
            mcpResponse.put("mcp_version", "1.0");

            return objectMapper.writeValueAsString(mcpResponse);
        } catch (Exception e) {
            // If the result is not valid JSON, return it as is with minimal MCP formatting
            log.warn("Could not parse tool result as JSON, returning raw result with minimal MCP formatting");

            StringBuilder mcpFormattedResult = new StringBuilder();
            mcpFormattedResult.append("MCP Tool Result [").append(tool.getName()).append("]:\n");
            mcpFormattedResult.append(result);

            return mcpFormattedResult.toString();
        }
    }

    /**
     * Create an MCP wrapper for a REST server.
     * This allows a REST server to be used through the MCP protocol in the chat interface.
     *
     * @param restServerUrl The URL of the REST server
     * @param restServerName The name to use for the REST server in the chat interface
     * @return A Mono containing the created external tool
     */
    public Mono<ExternalTool> createMCPWrapperForRESTServer(String restServerUrl, String restServerName) {
        ExternalTool mcpWrapper = ExternalTool.builder()
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

        return externalToolRepository.save(mcpWrapper);
    }
}
