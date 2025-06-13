package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Helper component for executing external tools directly without going through ExternalToolService.
 * This avoids circular dependencies between MCPService and ExternalToolService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MCPToolExecutor {

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    /**
     * Execute a tool request directly without going through ExternalToolService.
     *
     * @param tool The external tool to execute
     * @param parameters The parameters for the tool
     * @return A Mono containing the raw result
     */
    public Mono<String> executeToolRequest(ExternalTool tool, Map<String, Object> parameters) {
        try {
            WebClient webClient = webClientBuilder.build();
            
            // Prepare request body from template
            String requestBody = processRequestTemplate(tool.getRequestTemplate(), parameters);
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Build the URI with query parameters for GET requests
            String uri = tool.getEndpointUrl();
            if (tool.getHttpMethod() == ExternalTool.HttpMethod.GET && !parameters.isEmpty()) {
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);
                parameters.forEach((key, value) ->
                    builder.queryParam(key, value != null ? value.toString() : ""));
                uri = builder.toUriString();
            }
            
            // Execute the request based on HTTP method
            Mono<String> responseMono;
            switch (tool.getHttpMethod()) {
                case GET:
                    responseMono = webClient.get()
                        .uri(uri)
                        .headers(httpHeaders -> httpHeaders.addAll(headers))
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case POST:
                    responseMono = webClient.post()
                        .uri(uri)
                        .headers(httpHeaders -> httpHeaders.addAll(headers))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case PUT:
                    responseMono = webClient.put()
                        .uri(uri)
                        .headers(httpHeaders -> httpHeaders.addAll(headers))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case DELETE:
                    responseMono = webClient.delete()
                        .uri(uri)
                        .headers(httpHeaders -> httpHeaders.addAll(headers))
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                case PATCH:
                    responseMono = webClient.patch()
                        .uri(uri)
                        .headers(httpHeaders -> httpHeaders.addAll(headers))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class);
                    break;
                default:
                    return Mono.error(new RuntimeException("Unsupported HTTP method: " + tool.getHttpMethod()));
            }
            
            return responseMono
                .timeout(Duration.ofSeconds(30))
                .onErrorMap(e -> new RuntimeException("Error executing tool request: " + e.getMessage(), e));
                
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to prepare tool request: " + e.getMessage(), e));
        }
    }
    
    /**
     * Process a request template with parameters.
     */
    private String processRequestTemplate(String template, Map<String, Object> parameters) {
        if (template == null || template.trim().isEmpty()) {
            try {
                return objectMapper.writeValueAsString(parameters);
            } catch (Exception e) {
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
}