package com.darylmathison.chat.client.repository;

import com.darylmathison.chat.client.model.Chat;
import java.time.LocalDateTime;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRepository extends R2dbcRepository<Chat, Long> {

  Flux<Chat> findByCreatedAtBetweenOrderByUpdatedAtDesc(LocalDateTime start, LocalDateTime end);

  Flux<Chat> findByTitleContainingIgnoreCaseOrderByUpdatedAtDesc(String title);

  @Query("SELECT * FROM chats ORDER BY updated_at DESC")
  Flux<Chat> findAllOrderByUpdatedAtDesc();

  // Add this method that ChatService is calling
  Flux<Chat> findAllByOrderByUpdatedAtDesc();

  @Query("SELECT SUM(estimated_cost) FROM chats WHERE created_at >= :startDate")
  Mono<Double> getTotalCostSince(@Param("startDate") LocalDateTime startDate);

  // Add this method for monthly cost calculation
  Flux<Chat> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
}