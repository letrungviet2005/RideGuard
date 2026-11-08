package com.rideguard.users;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

public final class Responses {
  private Responses() {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record User(
      UUID id,
      String email,
      String fullName,
      String role,
      String status,
      Instant createdAt,
      Instant updatedAt) {}
}
