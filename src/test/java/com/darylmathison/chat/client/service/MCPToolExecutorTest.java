package com.darylmathison.chat.client.service;

import com.darylmathison.chat.client.model.ExternalTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MCPToolExecutorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private MCPToolExecutor mcpToolExecutor;

    @BeforeEach
    void setUp() {
        mcpToolExecutor = new MCPToolExecutor(objectMapper, webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void executeToolRequest_GET_SuccessfulExecution() throws Exception {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("Weather")
            .endpointUrl("https://api.example.com/weather")
            .httpMethod(ExternalTool.HttpMethod.GET)
            .isActive(true)
            .build();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("city", "New York");
        parameters.put("units", "metric");

        // Mock WebClient chain for GET request
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"temperature\": 22, \"conditions\": \"Sunny\"}"));

        // When & Then
        StepVerifier.create(mcpToolExecutor.executeToolRequest(tool, parameters))
            .expectNext("{\"temperature\": 22, \"conditions\": \"Sunny\"}")
            .verifyComplete();
    }

    @Test
    void executeToolRequest_POST_SuccessfulExecution() throws Exception {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("DataService")
            .endpointUrl("https://api.example.com/data")
            .httpMethod(ExternalTool.HttpMethod.POST)
            .requestTemplate("{\"query\": \"{{query}}\", \"limit\": {{limit}}}")
            .isActive(true)
            .build();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "search term");
        parameters.put("limit", 10);

        // Mock WebClient chain for POST request
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"results\": [\"item1\", \"item2\"]}"));

        // When & Then
        StepVerifier.create(mcpToolExecutor.executeToolRequest(tool, parameters))
            .expectNext("{\"results\": [\"item1\", \"item2\"]}")
            .verifyComplete();
    }

    @Test
    void executeToolRequest_UnsupportedMethod_ReturnsError() {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("InvalidMethod")
            .endpointUrl("https://api.example.com")
            .httpMethod(null) // Null HTTP method to trigger error
            .isActive(true)
            .build();

        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        StepVerifier.create(mcpToolExecutor.executeToolRequest(tool, parameters))
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("Failed to prepare tool request"))
            .verify();
    }

    @Test
    void executeToolRequest_EmptyTemplate_UsesParametersAsJson() throws Exception {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("SimpleService")
            .endpointUrl("https://api.example.com/simple")
            .httpMethod(ExternalTool.HttpMethod.POST)
            .requestTemplate("") // Empty template
            .isActive(true)
            .build();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");

        // Mock ObjectMapper for empty template
        when(objectMapper.writeValueAsString(parameters)).thenReturn("{\"key1\":\"value1\",\"key2\":\"value2\"}");

        // Mock WebClient chain for POST request
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Success"));

        // When & Then
        StepVerifier.create(mcpToolExecutor.executeToolRequest(tool, parameters))
            .expectNext("Success")
            .verifyComplete();
    }

    @Test
    void executeToolRequest_WebClientError_ReturnsError() {
        // Given
        ExternalTool tool = ExternalTool.builder()
            .id(1L)
            .name("ErrorService")
            .endpointUrl("https://api.example.com/error")
            .httpMethod(ExternalTool.HttpMethod.GET)
            .isActive(true)
            .build();

        Map<String, Object> parameters = new HashMap<>();

        // Mock WebClient chain for GET request with error
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // When & Then
        StepVerifier.create(mcpToolExecutor.executeToolRequest(tool, parameters))
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("Error executing tool request"))
            .verify();
    }
}
