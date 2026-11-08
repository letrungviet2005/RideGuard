package com.rideguard.devices;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

public final class Responses {
  private Responses() {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record AdminDevice(
      UUID id,
      String serialNumber,
      String model,
      String hardwareRevision,
      String status,
      Instant createdAt,
      Instant updatedAt,
      UUID activeBindingId) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Binding(
      UUID id,
      UUID deviceId,
      String serialNumber,
      String nickname,
      String model,
      String hardwareRevision,
      Instant boundAt,
      Instant unboundAt,
      String status) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Device(
      UUID id,
      UUID bindingId,
      String serialNumber,
      String nickname,
      String model,
      String hardwareRevision,
      String status,
      DeviceReport latestReport) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record DeviceConfig(
      UUID bindingId,
      Long schemaVersion,
      Long desiredVersion,
      Long reportedAppliedVersion,
      Settings settings) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record DeviceReport(
      UUID bindingId,
      Instant observedAt,
      String firmwareVersion,
      String radarStatus,
      String imuStatus,
      String displayStatus,
      String vibrationStatus,
      Long batteryPercent,
      Instant receivedAt) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record Settings(Integer displayBrightness, Boolean vibrationEnabled) {}
}
