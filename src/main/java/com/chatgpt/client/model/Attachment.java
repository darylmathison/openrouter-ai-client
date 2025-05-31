package com.chatgpt.client.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

  @Id
  private Long id;

  @Column("message_id")
  private Long messageId;

  @Column("file_name")
  private String fileName;

  @Column("file_path")
  private String filePath;

  @Column("file_type")
  private String fileType;

  @Column("file_size")
  private Long fileSize;

  @Column("content_type")
  private String contentType;

  @Column("is_processed")
  @Builder.Default
  private Boolean isProcessed = false;

  @Column("extracted_text")
  private String extractedText;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  public Attachment(Long messageId, String fileName, String filePath, String fileType,
      Long fileSize) {
    this.messageId = messageId;
    this.fileName = fileName;
    this.filePath = filePath;
    this.fileType = fileType;
    this.fileSize = fileSize;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.isProcessed = false;
  }

  public void markAsProcessed(String extractedText) {
    this.isProcessed = true;
    this.extractedText = extractedText;
    this.updatedAt = LocalDateTime.now();
  }

  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }
}