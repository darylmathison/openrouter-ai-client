package com.darylmathison.chat.client.repository;

import com.darylmathison.chat.client.model.SavedPrompt;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SavedPromptRepository extends R2dbcRepository<SavedPrompt, Long> {

  Flux<SavedPrompt> findByNameContainingIgnoreCaseOrderByUsageCountDesc(String name);

  @Query("SELECT * FROM saved_prompts WHERE " +
      "LOWER(prompt) LIKE LOWER(CONCAT('%', :content, '%')) OR " +
      "LOWER(description) LIKE LOWER(CONCAT('%', :content, '%')) " +
      "ORDER BY usage_count DESC")
  Flux<SavedPrompt> findByContentContaining(@Param("content") String content);

  Flux<SavedPrompt> findAllByOrderByUsageCountDesc();

  Flux<SavedPrompt> findAllByOrderByCreatedAtDesc();

  Flux<SavedPrompt> findByIsFavoriteOrderByUsageCountDesc(Boolean isFavorite);

  Flux<SavedPrompt> findByCategoryOrderByUsageCountDesc(String category);

  @Query("SELECT DISTINCT category FROM saved_prompts WHERE category IS NOT NULL ORDER BY category")
  Flux<String> findDistinctCategories();

  @Query("UPDATE saved_prompts SET usage_count = usage_count + 1, updated_at = NOW() WHERE id = :id")
  Mono<Integer> incrementUsageCount(@Param("id") Long id);
}