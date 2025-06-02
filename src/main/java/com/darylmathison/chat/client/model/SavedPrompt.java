package com.darylmathison.chat.client.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("saved_prompts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SavedPrompt {

  @Id
  private Long id;

  @Column("name")
  private String name;

  @Column("prompt")
  private String prompt;

  @Column("description")
  private String description;

  @Column("system_message")
  private String systemMessage;

  @Column("model_name")
  private String modelName;

  @Column("max_tokens")
  private Integer maxTokens;

  @Column("temperature")
  private Double temperature;

  @Column("category")
  private String category;

  @Column("usage_count")
  @Builder.Default
  private Long usageCount = 0L;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("is_favorite")
  @Builder.Default
  private Boolean isFavorite = false;

  @Column("tags")
  private String tags; // JSON string or comma-separated

  public SavedPrompt(String name, String prompt, String description) {
    this.name = name;
    this.prompt = prompt;
    this.description = description;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.usageCount = 0L;
    this.isFavorite = false;
  }

  public void incrementUsage() {
    this.usageCount++;
    this.updatedAt = LocalDateTime.now();
  }

  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }
}