package com.darylmathison.chat.client.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

  public enum MessageRole {
    USER, ASSISTANT, SYSTEM
  }

  @Id
  private Long id;

  @Column("chat_id")
  private Long chatId;

  @Column("content")
  private String content;

  @Column("role")
  private MessageRole role; // USER, ASSISTANT, SYSTEM

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("tokens")
  private Integer tokens;

  public Message(Long chatId, String content, MessageRole role) {
    this.chatId = chatId;
    this.content = content;
    this.role = role;
    this.createdAt = LocalDateTime.now();
  }
}