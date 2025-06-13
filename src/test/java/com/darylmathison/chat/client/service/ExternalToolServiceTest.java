package com.darylmathison.chat.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.darylmathison.chat.client.dto.ExternalToolDto;
import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.repository.ExternalToolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalToolServiceTest {

  @Mock
  private ExternalToolRepository externalToolRepository;

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient webClient;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private MCPService mcpService;

  private ExternalToolService externalToolService;

  @BeforeEach
  void setUp() {
    externalToolService = new ExternalToolService(externalToolRepository, webClientBuilder,
        objectMapper);
    when(webClientBuilder.build()).thenReturn(webClient);
  }

  @Test
  void saveTool_ShouldCreateNewTool() {
    // Given
    ExternalToolDto toolDto = ExternalToolDto.builder()
        .name("Test Tool")
        .description("Test Description")
        .endpointUrl("https://api.example.com")
        .httpMethod("GET")
        .authType("NONE")
        .isActive(true)
        .build();

    ExternalTool savedTool = ExternalTool.builder()
        .id(1L)
        .name("Test Tool")
        .description("Test Description")
        .endpointUrl("https://api.example.com")
        .httpMethod(ExternalTool.HttpMethod.GET)
        .authType(ExternalTool.AuthType.NONE)
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(externalToolRepository.save(any(ExternalTool.class))).thenReturn(Mono.just(savedTool));

    // When & Then
    StepVerifier.create(externalToolService.saveTool(toolDto))
        .expectNextMatches(result ->
            result.getName().equals("Test Tool") &&
                result.getId().equals(1L))
        .verifyComplete();
  }

  @Test
  void getActiveTools_ShouldReturnActiveTools() {
    // Given
    ExternalTool tool1 = ExternalTool.builder()
        .id(1L)
        .name("Tool 1")
        .isActive(true)
        .httpMethod(ExternalTool.HttpMethod.GET)
        .build();

    ExternalTool tool2 = ExternalTool.builder()
        .id(2L)
        .name("Tool 2")
        .isActive(true)
        .httpMethod(ExternalTool.HttpMethod.POST)
        .build();

    when(externalToolRepository.findByIsActiveTrueOrderByName())
        .thenReturn(Flux.just(tool1, tool2));

    // When & Then
    StepVerifier.create(externalToolService.getActiveTools())
        .expectNextMatches(tools -> tools.size() == 2)
        .verifyComplete();
  }

  @Test
  void executeTool_ShouldExecuteSuccessfully() {
    // Given
    Long toolId = 1L;
    Map<String, Object> parameters = Map.of("param1", "value1");

    ExternalTool tool = ExternalTool.builder()
        .id(toolId)
        .name("Test Tool")
        .endpointUrl("https://api.example.com")
        .httpMethod(ExternalTool.HttpMethod.GET)
        .authType(ExternalTool.AuthType.NONE)
        .isActive(true)
        .build();

    WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(
        WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(tool));
    when(externalToolRepository.recordUsage(toolId)).thenReturn(Mono.just(1));
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Success"));

    // When & Then
    StepVerifier.create(externalToolService.executeTool(toolId, parameters))
        .expectNext("Success")
        .verifyComplete();
  }

  @Test
  void executeTool_InactiveTool_ShouldThrowException() {
    // Given
    Long toolId = 1L;
    Map<String, Object> parameters = Map.of("param1", "value1");

    ExternalTool tool = ExternalTool.builder()
        .id(toolId)
        .name("Inactive Tool")
        .isActive(false)
        .build();

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(tool));

    // When & Then
    StepVerifier.create(externalToolService.executeTool(toolId, parameters))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void deleteTool_ShouldDeleteSuccessfully() {
    // Given
    Long toolId = 1L;
    ExternalTool tool = ExternalTool.builder().id(toolId).build();

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(tool));
    when(externalToolRepository.deleteById(toolId)).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(externalToolService.deleteTool(toolId))
        .verifyComplete();
  }

  @Test
  void updateTool_ShouldUpdateSuccessfully() {
    // Given
    Long toolId = 1L;
    ExternalToolDto toolDto = ExternalToolDto.builder()
        .name("Updated Tool")
        .description("Updated Description")
        .endpointUrl("https://api.updated.com")
        .httpMethod("POST")
        .isActive(true)
        .build();

    ExternalTool existingTool = ExternalTool.builder()
        .id(toolId)
        .name("Old Tool")
        .build();

    ExternalTool updatedTool = existingTool.toBuilder()
        .name("Updated Tool")
        .description("Updated Description")
        .endpointUrl("https://api.updated.com")
        .httpMethod(ExternalTool.HttpMethod.POST)
        .isActive(true)
        .updatedAt(LocalDateTime.now())
        .build();

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(existingTool));
    when(externalToolRepository.save(any(ExternalTool.class))).thenReturn(Mono.just(updatedTool));

    // When & Then
    StepVerifier.create(externalToolService.updateTool(toolId, toolDto))
        .expectNextMatches(result -> result.getName().equals("Updated Tool"))
        .verifyComplete();
  }

  @Test
  void getToolsByType_ShouldReturnToolsOfSpecifiedType() {
    // Given
    String toolType = "MCP";

    ExternalTool tool1 = ExternalTool.builder()
        .id(1L)
        .name("MCP Tool 1")
        .toolType(toolType)
        .isActive(true)
        .build();

    ExternalTool tool2 = ExternalTool.builder()
        .id(2L)
        .name("MCP Tool 2")
        .toolType(toolType)
        .isActive(true)
        .build();

    when(externalToolRepository.findByToolTypeAndIsActive(toolType, true))
        .thenReturn(Flux.just(tool1, tool2));

    // When & Then
    StepVerifier.create(externalToolService.getToolsByType(toolType))
        .expectNextMatches(tools -> 
            tools.size() == 2 && 
            tools.get(0).getName().equals("MCP Tool 1") &&
            tools.get(1).getName().equals("MCP Tool 2"))
        .verifyComplete();
  }

  @Test
  void setMcpService_ShouldSetMcpService() {
    // Given
    MCPService mcpService = mock(MCPService.class);

    // When
    externalToolService.setMcpService(mcpService);

    // Then
    // This is a bit tricky to test directly since mcpService is a private field
    // We'll test it indirectly by executing a tool with MCP enabled

    Long toolId = 1L;
    Map<String, Object> parameters = Map.of("input", "test input");
    String mcpResponse = "MCP response";

    ExternalTool mcpEnabledTool = ExternalTool.builder()
        .id(toolId)
        .name("MCP Tool")
        .isMcpEnabled(true)
        .isActive(true)
        .build();

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(mcpEnabledTool));
    when(mcpService.executeMCPTool(mcpEnabledTool, "test input")).thenReturn(Mono.just(mcpResponse));
    when(externalToolRepository.recordUsage(toolId)).thenReturn(Mono.just(1));

    StepVerifier.create(externalToolService.executeTool(toolId, parameters))
        .expectNext(mcpResponse)
        .verifyComplete();
  }

  @Test
  void executeTool_WithMcpEnabledTool_ShouldUseMcpService() {
    // Given
    Long toolId = 1L;
    Map<String, Object> parameters = Map.of("input", "test input");
    String mcpResponse = "MCP response";

    ExternalTool mcpEnabledTool = ExternalTool.builder()
        .id(toolId)
        .name("MCP Tool")
        .isMcpEnabled(true)
        .isActive(true)
        .build();

    // Set the MCPService
    externalToolService.setMcpService(mcpService);

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(mcpEnabledTool));
    when(mcpService.executeMCPTool(mcpEnabledTool, "test input")).thenReturn(Mono.just(mcpResponse));
    when(externalToolRepository.recordUsage(toolId)).thenReturn(Mono.just(1));

    // When & Then
    StepVerifier.create(externalToolService.executeTool(toolId, parameters))
        .expectNext(mcpResponse)
        .verifyComplete();
  }

  @Test
  void executeTool_WithMcpEnabledToolButNoMcpService_ShouldFallbackToStandardExecution() {
    // Given
    Long toolId = 1L;
    Map<String, Object> parameters = Map.of("param1", "value1");

    ExternalTool mcpEnabledTool = ExternalTool.builder()
        .id(toolId)
        .name("MCP Tool")
        .isMcpEnabled(true)
        .isActive(true)
        .endpointUrl("https://api.example.com")
        .httpMethod(ExternalTool.HttpMethod.GET)
        .build();

    // Don't set the MCPService to test fallback

    WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(
        WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    when(externalToolRepository.findById(toolId)).thenReturn(Mono.just(mcpEnabledTool));
    when(externalToolRepository.recordUsage(toolId)).thenReturn(Mono.just(1));
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Success"));

    // When & Then
    StepVerifier.create(externalToolService.executeTool(toolId, parameters))
        .expectNext("Success")
        .verifyComplete();
  }
}
