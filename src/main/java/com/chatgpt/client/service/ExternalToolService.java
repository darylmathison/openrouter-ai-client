package com.chatgpt.client.service;

import com.chatgpt.client.dto.ExternalToolDto;
import com.chatgpt.client.model.ExternalTool;
import com.chatgpt.client.repository.ExternalToolRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalToolService {

  private final ExternalToolRepository externalToolRepository;
  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper;

  public Mono<ExternalToolDto> saveTool(ExternalToolDto toolDto) {
    ExternalTool tool = ExternalTool.builder()
        .name(toolDto.getName())
        .description(toolDto.getDescription())
        .endpointUrl(toolDto.getEndpointUrl())
        .httpMethod(ExternalTool.HttpMethod.valueOf(toolDto.getHttpMethod()))
        .authType(toolDto.getAuthType() != null ?
            ExternalTool.AuthType.valueOf(toolDto.getAuthType()) :
            ExternalTool.AuthType.NONE)
        .authConfig(toolDto.getAuthConfig())
        .requestTemplate(toolDto.getRequestTemplate())
        .responseMapping(toolDto.getResponseMapping())
        .isActive(toolDto.getIsActive() != null ? toolDto.getIsActive() : true)
        .toolType(toolDto.getToolType() != null ? toolDto.getToolType() : "API")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .usageCount(0L)
        .build();

    return externalToolRepository.save(tool)
        .map(this::convertToDto)
        .doOnSuccess(savedTool -> log.info("Saved external tool: {}", savedTool.getName()))
        .doOnError(error -> log.error("Error saving external tool: {}", error.getMessage()));
  }

  public Mono<List<ExternalToolDto>> getActiveTools() {
    return externalToolRepository.findByIsActiveTrueOrderByName()
        .map(this::convertToDto)
        .collectList()
        .doOnSuccess(tools -> log.info("Retrieved {} active tools", tools.size()))
        .doOnError(error -> log.error("Error retrieving active tools: {}", error.getMessage()));
  }

  public Mono<String> executeTool(Long toolId, Map<String, Object> parameters) {
    return externalToolRepository.findById(toolId)
        .switchIfEmpty(
            Mono.error(new RuntimeException("External tool not found with id: " + toolId)))
        .flatMap(tool -> {
          if (!tool.getIsActive()) {
            return Mono.error(
                new RuntimeException("External tool is not active: " + tool.getName()));
          }
          return executeToolRequest(tool, parameters)
              .flatMap(response -> {
                // Record usage after successful execution
                return externalToolRepository.recordUsage(toolId)
                    .then(Mono.just(response));
              });
        })
        .doOnSuccess(response -> log.info("Successfully executed tool {}", toolId))
        .doOnError(error -> log.error("Error executing tool {}: {}", toolId, error.getMessage()));
  }

  public Mono<Void> deleteTool(Long toolId) {
    return externalToolRepository.findById(toolId)
        .switchIfEmpty(
            Mono.error(new RuntimeException("External tool not found with id: " + toolId)))
        .flatMap(tool -> externalToolRepository.deleteById(toolId))
        .doOnSuccess(v -> log.info("Deleted external tool with id: {}", toolId))
        .doOnError(error -> log.error("Error deleting tool {}: {}", toolId, error.getMessage()));
  }

  public Mono<ExternalToolDto> updateTool(Long toolId, ExternalToolDto toolDto) {
    return externalToolRepository.findById(toolId)
        .switchIfEmpty(
            Mono.error(new RuntimeException("External tool not found with id: " + toolId)))
        .flatMap(existingTool -> {
          ExternalTool updatedTool = existingTool.toBuilder()
              .name(toolDto.getName())
              .description(toolDto.getDescription())
              .endpointUrl(toolDto.getEndpointUrl())
              .httpMethod(ExternalTool.HttpMethod.valueOf(toolDto.getHttpMethod()))
              .authType(toolDto.getAuthType() != null ?
                  ExternalTool.AuthType.valueOf(toolDto.getAuthType()) :
                  ExternalTool.AuthType.NONE)
              .authConfig(toolDto.getAuthConfig())
              .requestTemplate(toolDto.getRequestTemplate())
              .responseMapping(toolDto.getResponseMapping())
              .isActive(toolDto.getIsActive())
              .toolType(toolDto.getToolType())
              .updatedAt(LocalDateTime.now())
              .build();

          return externalToolRepository.save(updatedTool);
        })
        .map(this::convertToDto)
        .doOnSuccess(updatedTool -> log.info("Updated external tool: {}", updatedTool.getName()))
        .doOnError(error -> log.error("Error updating tool {}: {}", toolId, error.getMessage()));
  }

  private Mono<String> executeToolRequest(ExternalTool tool, Map<String, Object> parameters) {
    return Mono.fromCallable(() -> {
          try {
            WebClient webClient = webClientBuilder.build();

            // Prepare request body from template
            String requestBody = processRequestTemplate(tool.getRequestTemplate(), parameters);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            addAuthHeaders(headers, tool);

            // Build the URI with query parameters for GET requests
            String uri = buildUriWithParameters(tool.getEndpointUrl(), tool.getHttpMethod(),
                parameters);

            return switch (tool.getHttpMethod()) {
              case GET -> webClient.get()
                  .uri(uri)
                  .headers(httpHeaders -> httpHeaders.addAll(headers))
                  .retrieve()
                  .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                  .bodyToMono(String.class);
              case POST -> webClient.post()
                  .uri(tool.getEndpointUrl())
                  .headers(httpHeaders -> httpHeaders.addAll(headers))
                  .bodyValue(requestBody)
                  .retrieve()
                  .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                  .bodyToMono(String.class);
              case PUT -> webClient.put()
                  .uri(tool.getEndpointUrl())
                  .headers(httpHeaders -> httpHeaders.addAll(headers))
                  .bodyValue(requestBody)
                  .retrieve()
                  .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                  .bodyToMono(String.class);
              case DELETE -> webClient.delete()
                  .uri(tool.getEndpointUrl())
                  .headers(httpHeaders -> httpHeaders.addAll(headers))
                  .retrieve()
                  .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                  .bodyToMono(String.class);
              case PATCH -> webClient.patch()
                  .uri(tool.getEndpointUrl())
                  .headers(httpHeaders -> httpHeaders.addAll(headers))
                  .bodyValue(requestBody)
                  .retrieve()
                  .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                  .bodyToMono(String.class);
            };
          } catch (Exception e) {
            log.error("Error preparing request for external tool: {}", tool.getName(), e);
            throw new RuntimeException("Failed to prepare external tool request", e);
          }
        })
        .flatMap(responseMono -> responseMono)
        .map(response -> processResponse(response, tool.getResponseMapping(), parameters))
        .onErrorMap(WebClientResponseException.class, ex ->
            new RuntimeException("External tool request failed: " + ex.getMessage(), ex))
        .timeout(Duration.ofSeconds(30))
        .doOnError(error -> log.error("Error executing external tool {}: {}", tool.getName(),
            error.getMessage()));
  }

  private Mono<Throwable> handleErrorResponse(ClientResponse response) {
    return response.bodyToMono(String.class)
        .defaultIfEmpty("Unknown error")
        .map(errorBody -> new RuntimeException(
            String.format("External tool request failed with status %d: %s",
                response.statusCode().value(), errorBody)));
  }

  private String buildUriWithParameters(String baseUrl, ExternalTool.HttpMethod method,
      Map<String, Object> parameters) {
    if (method != ExternalTool.HttpMethod.GET || parameters.isEmpty()) {
      return baseUrl;
    }

    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
    parameters.forEach((key, value) ->
        builder.queryParam(key, value != null ? value.toString() : ""));

    return builder.toUriString();
  }

  private String processRequestTemplate(String template, Map<String, Object> parameters) {
    if (template == null || template.trim().isEmpty()) {
      try {
        return objectMapper.writeValueAsString(parameters);
      } catch (JsonProcessingException e) {
        log.warn("Failed to serialize parameters to JSON: {}", e.getMessage());
        return "{}";
      }
    }

    String processed = template;
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      String value = entry.getValue() != null ? entry.getValue().toString() : "";
      processed = processed.replace(placeholder, value);
    }

    return processed;
  }

  private void addAuthHeaders(HttpHeaders headers, ExternalTool tool) {
    if (tool.getAuthType() == ExternalTool.AuthType.NONE || tool.getAuthConfig() == null) {
      return;
    }

    try {
      JsonNode authConfig = objectMapper.readTree(tool.getAuthConfig());

      switch (tool.getAuthType()) {
        case API_KEY -> {
          String apiKey = authConfig.get("apiKey").asText();
          String headerName = authConfig.has("headerName") ?
              authConfig.get("headerName").asText() : "X-API-Key";
          headers.add(headerName, apiKey);
        }
        case BEARER_TOKEN -> {
          String token = authConfig.get("token").asText();
          headers.setBearerAuth(token);
        }
        case BASIC_AUTH -> {
          String username = authConfig.get("username").asText();
          String password = authConfig.get("password").asText();
          headers.setBasicAuth(username, password);
        }
        case OAUTH2 -> {
          String token = authConfig.get("accessToken").asText();
          headers.setBearerAuth(token);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse auth config for tool: {}", tool.getName(), e);
    }
  }

  private String processResponse(String response, String responseMapping,
      Map<String, Object> parameters) {
    if (responseMapping == null || responseMapping.trim().isEmpty()) {
      return response;
    }

    try {
      JsonNode responseNode = objectMapper.readTree(response);
      JsonNode mappingNode = objectMapper.readTree(responseMapping);

      if (mappingNode.has("extract")) {
        String extractPath = mappingNode.get("extract").asText();
        JsonNode extractedValue = responseNode.at(extractPath);
        return extractedValue.isTextual() ? extractedValue.asText() : extractedValue.toString();
      }

      return response;
    } catch (Exception e) {
      log.warn("Failed to process response mapping, returning raw response: {}", e.getMessage());
      return response;
    }
  }

  private ExternalToolDto convertToDto(ExternalTool tool) {
    return ExternalToolDto.builder()
        .id(tool.getId())
        .name(tool.getName())
        .description(tool.getDescription())
        .endpointUrl(tool.getEndpointUrl())
        .httpMethod(tool.getHttpMethod().name())
        .authType(tool.getAuthType().name())
        .authConfig(tool.getAuthConfig())
        .requestTemplate(tool.getRequestTemplate())
        .responseMapping(tool.getResponseMapping())
        .isActive(tool.getIsActive())
        .toolType(tool.getToolType())
        .usageCount(tool.getUsageCount())
        .lastUsedAt(tool.getLastUsedAt())
        .createdAt(tool.getCreatedAt())
        .updatedAt(tool.getUpdatedAt())
        // Backward compatibility
        .baseUrl(tool.getBaseUrl())
        .apiKey(tool.getApiKey())
        .isEnabled(tool.getIsEnabled())
        .configuration(tool.getConfiguration())
        .build();
  }
}