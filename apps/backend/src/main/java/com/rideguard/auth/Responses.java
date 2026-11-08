package com.rideguard.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

public final class Responses {
  private Responses() {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Session(
      UUID id,
      String clientType,
      String clientName,
      Instant createdAt,
      Instant lastSeenAt,
      Instant expiresAt,
      Boolean isCurrent) {}
}
