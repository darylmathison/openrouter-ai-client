package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing messages and detecting external tool calls.
 */
@Service
@Slf4j
public class MessageParserService {

    private final ExternalToolRepository externalToolRepository;
    private final ExternalToolService externalToolService;
    private MCPService mcpService;

    // Pattern to match @{{name}} format
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("@\\{\\{([^}]+)\\}\\}\\s*(.*)");

    @Autowired
    public MessageParserService(
        ExternalToolRepository externalToolRepository,
        ExternalToolService externalToolService) {
        this.externalToolRepository = externalToolRepository;
        this.externalToolService = externalToolService;
    }

    @Autowired
    public void setMcpService(@Lazy MCPService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * Parse a message and detect external tool calls.
     * If a tool call is detected, execute the tool using MCP if available, or fall back to the standard execution.
     * Otherwise, return the original message.
     *
     * @param message The message to parse
     * @return A Mono containing the processed message
     */
    public Mono<String> parseAndProcessMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Mono.just("");
        }

        // If MCP service is available, delegate to it
        if (mcpService != null) {
            return mcpService.parseAndProcessMessage(message);
        }

        // Fall back to legacy implementation if MCP service is not available
        Matcher matcher = TOOL_CALL_PATTERN.matcher(message);
        if (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String toolInput = matcher.group(2).trim();

            log.info("Detected tool call (legacy mode): tool={}, input={}", toolName, toolInput);

            return externalToolRepository.findByNameIgnoreCase(toolName)
                .switchIfEmpty(Mono.error(new RuntimeException("External tool not found: " + toolName)))
                .flatMap(tool -> {
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put("input", toolInput);

                    return externalToolService.executeTool(tool.getId(), parameters);
                })
                .doOnSuccess(result -> log.info("Tool execution successful (legacy mode): {}", result))
                .doOnError(error -> log.error("Error executing tool (legacy mode): {}", error.getMessage()));
        }

        return Mono.just(message);
    }
}
