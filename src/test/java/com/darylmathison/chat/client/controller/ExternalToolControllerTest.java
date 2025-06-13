package com.darylmathison.chat.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.darylmathison.chat.client.config.TestSecurityConfig;
import com.darylmathison.chat.client.dto.ExternalToolDto;
import com.darylmathison.chat.client.model.ExternalTool;
import com.darylmathison.chat.client.service.ExternalToolService;
import com.darylmathison.chat.client.service.MCPService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(ExternalToolController.class)
@Import(TestSecurityConfig.class)
class ExternalToolControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private ExternalToolService externalToolService;

  @MockBean
  private MCPService mcpService;

  @Test
  void saveTool_ShouldReturnCreatedTool() {
    // Given
    ExternalToolDto toolDto = ExternalToolDto.builder()
        .name("Test Tool")
        .endpointUrl("https://api.example.com")
        .httpMethod("GET")
        .build();

    ExternalToolDto savedTool = toolDto.toBuilder().id(1L).build();

    when(externalToolService.saveTool(any(ExternalToolDto.class)))
        .thenReturn(Mono.just(savedTool));

    // When & Then
    webTestClient.post()
        .uri("/api/tools")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(toolDto)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ExternalToolDto.class)
        .value(tool -> {
          assertThat(tool.getId()).isEqualTo(1L);
          assertThat(tool.getName()).isEqualTo("Test Tool");
        });
  }

  @Test
  void getActiveTools_ShouldReturnToolsList() {
    // Given
    List<ExternalToolDto> tools = List.of(
        ExternalToolDto.builder().id(1L).name("Tool 1").build(),
        ExternalToolDto.builder().id(2L).name("Tool 2").build()
    );

    when(externalToolService.getActiveTools()).thenReturn(Mono.just(tools));

    // When & Then
    webTestClient.get()
        .uri("/api/tools")
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(ExternalToolDto.class)
        .hasSize(2);
  }

  @Test
  void executeTool_ShouldReturnResponse() {
    // Given
    Long toolId = 1L;
    Map<String, Object> parameters = Map.of("param1", "value1");
    String expectedResponse = "Tool execution result";

    when(externalToolService.executeTool(eq(toolId), any()))
        .thenReturn(Mono.just(expectedResponse));

    // When & Then
    webTestClient.post()
        .uri("/api/tools/{toolId}/execute", toolId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(parameters)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .isEqualTo(expectedResponse);
  }

  @Test
  void deleteTool_ShouldReturnNoContent() {
    // Given
    Long toolId = 1L;
    when(externalToolService.deleteTool(toolId)).thenReturn(Mono.empty());

    // When & Then
    webTestClient.delete()
        .uri("/api/tools/{toolId}", toolId)
        .exchange()
        .expectStatus().isNoContent();
  }

  @Test
  void createMCPWrapper_ShouldReturnCreatedWrapper() {
    // Given
    String restServerUrl = "https://api.example.com";
    String restServerName = "ExampleAPI";

    ExternalTool mcpWrapper = ExternalTool.builder()
        .id(1L)
        .name(restServerName)
        .description("MCP wrapper for REST server: " + restServerUrl)
        .endpointUrl(restServerUrl)
        .toolType("MCP_REST_WRAPPER")
        .build();

    ExternalToolDto mcpWrapperDto = ExternalToolDto.builder()
        .id(1L)
        .name(restServerName)
        .description("MCP wrapper for REST server: " + restServerUrl)
        .endpointUrl(restServerUrl)
        .toolType("MCP_REST_WRAPPER")
        .build();

    when(mcpService.createMCPWrapperForRESTServer(restServerUrl, restServerName))
        .thenReturn(Mono.just(mcpWrapper));
    when(externalToolService.convertToDto(mcpWrapper)).thenReturn(mcpWrapperDto);

    // When & Then
    webTestClient.post()
        .uri(uriBuilder -> uriBuilder
            .path("/api/tools/mcp-wrapper")
            .queryParam("restServerUrl", restServerUrl)
            .queryParam("restServerName", restServerName)
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody(ExternalToolDto.class)
        .value(wrapper -> {
            assertThat(wrapper.getId()).isEqualTo(1L);
            assertThat(wrapper.getName()).isEqualTo(restServerName);
            assertThat(wrapper.getToolType()).isEqualTo("MCP_REST_WRAPPER");
        });
  }

  @Test
  void getMCPTools_ShouldReturnMCPTools() {
    // Given
    List<ExternalToolDto> mcpTools = List.of(
        ExternalToolDto.builder().id(1L).name("MCP Tool 1").toolType("MCP").build(),
        ExternalToolDto.builder().id(2L).name("MCP Tool 2").toolType("MCP").build()
    );

    when(externalToolService.getToolsByType("MCP")).thenReturn(Mono.just(mcpTools));

    // When & Then
    webTestClient.get()
        .uri("/api/tools/mcp")
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(ExternalToolDto.class)
        .hasSize(2)
        .contains(mcpTools.toArray(new ExternalToolDto[0]));
  }

  @Test
  void getMCPWrappers_ShouldReturnMCPWrappers() {
    // Given
    List<ExternalToolDto> mcpWrappers = List.of(
        ExternalToolDto.builder().id(1L).name("MCP Wrapper 1").toolType("MCP_REST_WRAPPER").build(),
        ExternalToolDto.builder().id(2L).name("MCP Wrapper 2").toolType("MCP_REST_WRAPPER").build()
    );

    when(externalToolService.getToolsByType("MCP_REST_WRAPPER")).thenReturn(Mono.just(mcpWrappers));

    // When & Then
    webTestClient.get()
        .uri("/api/tools/mcp-wrappers")
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(ExternalToolDto.class)
        .hasSize(2)
        .contains(mcpWrappers.toArray(new ExternalToolDto[0]));
  }
}
