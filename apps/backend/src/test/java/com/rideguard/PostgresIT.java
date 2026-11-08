package com.rideguard;

import java.util.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
abstract class PostgresIT {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    String url = System.getenv("TEST_DB_URL");
    if (url == null || !url.matches("jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test"))
      throw new IllegalStateException(
          "Use a dedicated database ending in _test; query parameters are not allowed");
    String key = Base64.getEncoder().encodeToString(new byte[32]);
    r.add("spring.datasource.url", () -> url);
    r.add(
        "spring.datasource.username", () -> Objects.requireNonNull(System.getenv("TEST_DB_USER")));
    r.add(
        "spring.datasource.password",
        () -> Objects.requireNonNull(System.getenv("TEST_DB_PASSWORD")));
    r.add("rideguard.jwt-key", () -> key);
    r.add("rideguard.cursor-key", () -> Base64.getEncoder().encodeToString(new byte[48]));
    r.add("rideguard.allowed-origins", () -> "https://admin.example.invalid");
    r.add("rideguard.rate-limit-per-minute", () -> 10000);
  }
}
