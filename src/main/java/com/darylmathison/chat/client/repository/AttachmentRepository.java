package com.darylmathison.chat.client.repository;

import com.darylmathison.chat.client.model.Attachment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AttachmentRepository extends R2dbcRepository<Attachment, Long> {

  Flux<Attachment> findByMessageIdOrderByCreatedAtAsc(Long messageId);

  Flux<Attachment> findByIsProcessedOrderByCreatedAtDesc(Boolean isProcessed);

  Flux<Attachment> findByFileTypeOrderByCreatedAtDesc(String fileType);

  @Query("SELECT a.* FROM attachments a " +
      "INNER JOIN messages m ON a.message_id = m.id " +
      "WHERE m.chat_id = :chatId " +
      "ORDER BY a.created_at ASC")
  Flux<Attachment> findByChatIdOrderByCreatedAtAsc(@Param("chatId") Long chatId);

  @Query("SELECT SUM(file_size) FROM attachments WHERE message_id IN " +
      "(SELECT id FROM messages WHERE chat_id = :chatId)")
  Mono<Long> getTotalFileSizeByChatId(@Param("chatId") Long chatId);

  Mono<Void> deleteByMessageId(Long messageId);

  @Query("SELECT COUNT(*) FROM attachments WHERE is_processed = false")
  Mono<Long> countUnprocessedAttachments();
}
