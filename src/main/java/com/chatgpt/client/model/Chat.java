package com.chatgpt.client.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("chats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

  @Id
  private Long id;

  @Column("title")
  private String title;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("total_tokens")
  private Long totalTokens;

  @Column("estimated_cost")
  private Double estimatedCost;

  @Column("model_used")
  private String modelUsed;

  // R2DBC doesn't support @OneToMany relationships directly
  // We'll load messages separately using the repository
  @Transient
  private List<Message> messages;

  public Chat(String title) {
    this.title = title;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }
}