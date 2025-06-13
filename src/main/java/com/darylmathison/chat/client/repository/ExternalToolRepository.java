package com.darylmathison.chat.client.repository;

import com.darylmathison.chat.client.model.ExternalTool;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ExternalToolRepository extends R2dbcRepository<ExternalTool, Long> {

  // Updated to use isActive instead of isEnabled
  Flux<ExternalTool> findByIsActiveTrueOrderByName();

  Flux<ExternalTool> findByIsActiveOrderByNameAsc(Boolean isActive);

  Flux<ExternalTool> findByToolTypeOrderByUsageCountDesc(String toolType);

  /**
   * Find tools by type and active status.
   * 
   * @param toolType The type of tools to find
   * @param isActive Whether to find active or inactive tools
   * @return A Flux of tools matching the criteria
   */
  Flux<ExternalTool> findByToolTypeAndIsActive(String toolType, Boolean isActive);

  Flux<ExternalTool> findByNameContainingIgnoreCaseOrderByUsageCountDesc(String name);

  /**
   * Find a tool by its exact name (case insensitive).
   * 
   * @param name The name of the tool to find
   * @return A Mono containing the tool if found, or an empty Mono if not found
   */
  @Query("SELECT * FROM external_tools WHERE LOWER(name) = LOWER(:name) LIMIT 1")
  Mono<ExternalTool> findByNameIgnoreCase(@Param("name") String name);

  @Query("SELECT * FROM external_tools WHERE is_active = true ORDER BY usage_count DESC")
  Flux<ExternalTool> findActiveToolsOrderByUsage();

  @Query("UPDATE external_tools SET usage_count = usage_count + 1, " +
      "last_used_at = NOW(), updated_at = NOW() WHERE id = :id")
  Mono<Integer> recordUsage(@Param("id") Long id);

  @Query("SELECT DISTINCT tool_type FROM external_tools ORDER BY tool_type")
  Flux<String> findDistinctToolTypes();

  // Backward compatibility methods
  @Query("SELECT * FROM external_tools WHERE (is_enabled = true OR is_active = true) ORDER BY name")
  Flux<ExternalTool> findEnabledToolsOrderByUsage();
}
