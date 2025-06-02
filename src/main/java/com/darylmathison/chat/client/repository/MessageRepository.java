package com.darylmathison.chat.client.repository;

import com.darylmathison.chat.client.model.Message;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository extends R2dbcRepository<Message, Long> {

  Flux<Message> findByChatIdOrderByCreatedAtAsc(Long chatId);

  Mono<Long> countByChatId(Long chatId);

  Mono<Message> findTopByChatIdOrderByCreatedAtDesc(Long chatId);

  Mono<Void> deleteByChatId(Long chatId);
}
