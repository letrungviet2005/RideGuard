package com.rideguard.devices;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;

public final class DeviceDtos {
  private DeviceDtos() {}

  public record Bind(String serial, String nickname) {
    public static Bind parse(JsonNode n) {
      Json.fields(n, "serial_number,nickname", "serial_number");
      return new Bind(Json.serial(n), Json.str(n, "nickname", 1, 60, true, true));
    }
  }

  public record Create(String serial, String model, String revision) {
    public static Create parse(JsonNode n) {
      Json.fields(n, "serial_number,model,hardware_revision", "serial_number,model");
      return new Create(
          Json.serial(n),
          Json.str(n, "model", 1, 100, false, true),
          Json.str(n, "hardware_revision", 1, 64, true, true));
    }
  }

  public record ConfigPatch(long expected, Map<String, Object> settings) {
    public static ConfigPatch parse(JsonNode n) {
      Json.fields(n, "expected_version,settings", "expected_version,settings");
      long v = Json.number(n, "expected_version", 1, 9007199254740991L);
      JsonNode s = n.get("settings");
      Json.fields(s, "display_brightness,vibration_enabled", "");
      if (s.isEmpty()) throw ApiException.bad();
      Map<String, Object> m = new LinkedHashMap<>();
      if (s.has("display_brightness"))
        m.put("display_brightness", Json.number(s, "display_brightness", 0, 100));
      if (s.has("vibration_enabled")) {
        if (!s.get("vibration_enabled").isBoolean()) throw ApiException.bad();
        m.put("vibration_enabled", s.get("vibration_enabled").booleanValue());
      }
      return new ConfigPatch(v, m);
    }
  }

  public record Report(
      UUID binding,
      Instant observed,
      String firmware,
      String radar,
      String imu,
      String display,
      String vibration,
      Integer battery) {
    public static Report parse(JsonNode n) {
      String fields =
          "binding_id,observed_at,firmware_version,radar_status,imu_status,display_status,vibration_status,battery_percent";
      Json.fields(n, fields, fields);
      String states = "OK,ERROR,UNAVAILABLE,UNKNOWN";
      return new Report(
          Json.uuid(n, "binding_id", false),
          Json.instant(n, "observed_at"),
          Json.str(n, "firmware_version", 1, 64, true, true),
          Json.choice(n, "radar_status", states),
          Json.choice(n, "imu_status", states),
          Json.choice(n, "display_status", states),
          Json.choice(n, "vibration_status", states),
          n.get("battery_percent").isNull()
              ? null
              : (int) Json.number(n, "battery_percent", 0, 100));
    }
  }
}
