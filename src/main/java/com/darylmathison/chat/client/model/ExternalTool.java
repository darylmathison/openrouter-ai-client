package com.darylmathison.chat.client.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("external_tools")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ExternalTool {

  @Id
  private Long id;

  @Column("name")
  private String name;

  @Column("description")
  private String description;

  @Column("endpoint_url")
  private String endpointUrl;

  @Column("http_method")
  private HttpMethod httpMethod;

  @Column("auth_type")
  @Builder.Default
  private AuthType authType = AuthType.NONE;

  @Column("auth_config")
  private String authConfig; // JSON string for auth configuration

  @Column("request_template")
  private String requestTemplate; // JSON template for requests

  @Column("response_mapping")
  private String responseMapping; // JSON mapping for response processing

  @Column("is_active")
  @Builder.Default
  private Boolean isActive = true;

  @Column("tool_type")
  private String toolType; // API, WEBHOOK, MCP, MCP_REST_WRAPPER, etc.

  @Column("is_mcp_enabled")
  @Builder.Default
  private Boolean isMcpEnabled = true; // Whether this tool uses the MCP protocol

  @Column("mcp_config")
  private String mcpConfig; // JSON string for MCP-specific configuration

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("last_used_at")
  private LocalDateTime lastUsedAt;

  @Column("usage_count")
  @Builder.Default
  private Long usageCount = 0L;

  // Keep backward compatibility fields
  @Column("base_url")
  private String baseUrl; // Deprecated, use endpointUrl

  @Column("api_key")
  private String apiKey; // Deprecated, use authConfig

  @Column("is_enabled")
  private Boolean isEnabled; // Deprecated, use isActive

  @Column("configuration")
  private String configuration; // Deprecated, use requestTemplate/responseMapping

  public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH
  }

  public enum AuthType {
    NONE, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2
  }

  public ExternalTool(String name, String description, String endpointUrl, String toolType) {
    this.name = name;
    this.description = description;
    this.endpointUrl = endpointUrl;
    this.toolType = toolType;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.isActive = true;
    this.usageCount = 0L;
    this.authType = AuthType.NONE;
    this.httpMethod = HttpMethod.GET;
  }

  public void recordUsage() {
    this.usageCount++;
    this.lastUsedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }

  // Backward compatibility methods
  public String getBaseUrl() {
    return baseUrl != null ? baseUrl : endpointUrl;
  }

  public Boolean getIsEnabled() {
    return isEnabled != null ? isEnabled : isActive;
  }
}
