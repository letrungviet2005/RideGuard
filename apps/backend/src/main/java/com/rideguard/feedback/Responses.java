package com.rideguard.feedback;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

public final class Responses {
  private Responses() {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record AdminFeedback(
      UUID id,
      UUID deviceId,
      String category,
      String content,
      String status,
      String adminResponse,
      Instant createdAt,
      Instant respondedAt,
      UUID userId,
      UUID bindingId) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Feedback(
      UUID id,
      UUID deviceId,
      String category,
      String content,
      String status,
      String adminResponse,
      Instant createdAt,
      Instant respondedAt) {}
}
