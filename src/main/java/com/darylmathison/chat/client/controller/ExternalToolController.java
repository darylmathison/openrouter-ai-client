package com.darylmathison.chat.client.controller;

import com.darylmathison.chat.client.dto.ExternalToolDto;
import com.darylmathison.chat.client.service.ExternalToolService;
import com.darylmathison.chat.client.service.MCPService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExternalToolController {

  private final ExternalToolService externalToolService;
  private final MCPService mcpService;

  @PostMapping
  public Mono<ResponseEntity<ExternalToolDto>> saveTool(
      @Valid @RequestBody ExternalToolDto toolDto) {
    return externalToolService.saveTool(toolDto)
        .map(ResponseEntity::ok);
  }

  @GetMapping
  public Mono<ResponseEntity<List<ExternalToolDto>>> getActiveTools() {
    return externalToolService.getActiveTools()
        .map(ResponseEntity::ok);
  }

  @PostMapping("/{toolId}/execute")
  public Mono<ResponseEntity<String>> executeTool(
      @PathVariable Long toolId,
      @RequestBody Map<String, Object> parameters) {
    return externalToolService.executeTool(toolId, parameters)
        .map(ResponseEntity::ok);
  }

  @PutMapping("/{toolId}")
  public Mono<ResponseEntity<ExternalToolDto>> updateTool(
      @PathVariable Long toolId,
      @Valid @RequestBody ExternalToolDto toolDto) {
    return externalToolService.updateTool(toolId, toolDto)
        .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{toolId}")
  public Mono<ResponseEntity<Void>> deleteTool(@PathVariable Long toolId) {
    return externalToolService.deleteTool(toolId)
        .then(Mono.just(ResponseEntity.noContent().build()));
  }

  /**
   * Create an MCP wrapper for a REST server.
   * This allows a REST server to be used through the MCP protocol in the chat interface.
   *
   * @param restServerUrl The URL of the REST server
   * @param restServerName The name to use for the REST server in the chat interface
   * @return A ResponseEntity containing the created external tool
   */
  @PostMapping("/mcp-wrapper")
  public Mono<ResponseEntity<ExternalToolDto>> createMCPWrapper(
      @RequestParam String restServerUrl,
      @RequestParam String restServerName) {
    return mcpService.createMCPWrapperForRESTServer(restServerUrl, restServerName)
        .map(externalToolService::convertToDto)
        .map(ResponseEntity::ok);
  }

  /**
   * Get all MCP-enabled tools.
   *
   * @return A ResponseEntity containing a list of MCP-enabled tools
   */
  @GetMapping("/mcp")
  public Mono<ResponseEntity<List<ExternalToolDto>>> getMCPTools() {
    return externalToolService.getToolsByType("MCP")
        .map(tools -> ResponseEntity.ok(tools));
  }

  /**
   * Get all MCP REST wrapper tools.
   *
   * @return A ResponseEntity containing a list of MCP REST wrapper tools
   */
  @GetMapping("/mcp-wrappers")
  public Mono<ResponseEntity<List<ExternalToolDto>>> getMCPWrappers() {
    return externalToolService.getToolsByType("MCP_REST_WRAPPER")
        .map(tools -> ResponseEntity.ok(tools));
  }
}
