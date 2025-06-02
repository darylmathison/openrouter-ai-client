package com.darylmathison.chat.client.controller;

import com.darylmathison.chat.client.dto.ExternalToolDto;
import com.darylmathison.chat.client.service.ExternalToolService;
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
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExternalToolController {

  private final ExternalToolService externalToolService;

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
}