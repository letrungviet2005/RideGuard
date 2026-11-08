package com.rideguard;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.rideguard.auth.*;
import com.rideguard.devices.*;
import com.rideguard.feedback.*;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ContractRulesTest {
  @Test
  void responseDtosPreserveSnakeCaseNullsAndUtc() throws Exception {
    var mapper =
        org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    var views = new ResponseViews(mapper);
    UUID id = UUID.randomUUID();
    var config =
        views.one(
            Json.obj(
                "binding_id",
                id,
                "schema_version",
                1,
                "desired_version",
                1,
                "reported_applied_version",
                null,
                "settings",
                Json.obj("display_brightness", 70, "vibration_enabled", false)),
            com.rideguard.devices.Responses.DeviceConfig.class);
    JsonNode json = mapper.valueToTree(config);
    assertTrue(json.has("reported_applied_version"));
    assertTrue(json.path("reported_applied_version").isNull());
    assertFalse(json.path("settings").path("vibration_enabled").asBoolean());
    var profile =
        views.one(
            Json.obj(
                "id",
                id,
                "email",
                "a@b.invalid",
                "full_name",
                "A",
                "role",
                "USER",
                "status",
                "ACTIVE",
                "created_at",
                Instant.parse("2026-11-07T00:00:00Z"),
                "updated_at",
                Instant.parse("2026-11-07T00:00:00Z")),
            com.rideguard.users.Responses.User.class);
    assertEquals("2026-11-07T00:00:00Z", mapper.valueToTree(profile).path("created_at").asText());
  }

  private final ObjectMapper m = new ObjectMapper();

  private JsonNode n(String s) throws Exception {
    return m.readTree(s);
  }

  @Test
  void canonicalizationAndPasswordPreservation() {
    var d =
        AuthDtos.Register.parse(
            m.valueToTree(
                Json.obj(
                    "email",
                    " A@Example.invalid ",
                    "full_name",
                    " A ",
                    "password",
                    " 1234567890 ")));
    assertEquals("a@example.invalid", d.email());
    assertEquals("A", d.fullName());
    assertEquals(" 1234567890 ", d.password());
  }

  @Test
  void roleInjectionAndWrongTypesRejected() throws Exception {
    for (String s :
        List.of(
            "{\"email\":\"a@b.invalid\",\"full_name\":\"A\",\"password\":\"123456789012\",\"role\":\"ADMIN\"}",
            "{\"email\":1,\"full_name\":\"A\",\"password\":\"123456789012\"}")) {
      JsonNode x = n(s);
      assertThrows(ApiException.class, () -> AuthDtos.Register.parse(x));
    }
  }

  @Test
  void passwordsCountUnicodeWithoutTrim() {
    String password = "\uD83D\uDE00".repeat(12);
    var d =
        AuthDtos.Register.parse(
            m.valueToTree(
                Json.obj("email", "a@b.invalid", "full_name", "A", "password", password)));
    assertEquals(password, d.password());
    assertThrows(
        ApiException.class,
        () ->
            AuthDtos.Register.parse(
                m.valueToTree(
                    Json.obj(
                        "email", "a@b.invalid", "full_name", "A", "password", "x".repeat(129)))));
  }

  @Test
  void serialAsciiOnly() throws Exception {
    assertEquals("RG-1", DeviceDtos.Bind.parse(n("{\"serial_number\":\" rg-1 \"}")).serial());
    for (String bad : List.of("RG 1", "ÃŸ", "-1", "ï¼²ï¼§")) {
      assertThrows(
          ApiException.class,
          () -> DeviceDtos.Bind.parse(m.valueToTree(Json.obj("serial_number", bad))));
    }
  }

  @Test
  void settingsRejectUnknownNullFractionAndEmpty() throws Exception {
    for (String settings :
        List.of(
            "{}",
            "{\"vibration_enabled\":null}",
            "{\"display_brightness\":1.2}",
            "{\"display_brightness\":101}",
            "{\"risk_threshold\":1}")) {
      JsonNode x = n("{\"expected_version\":1,\"settings\":" + settings + "}");
      assertThrows(ApiException.class, () -> DeviceDtos.ConfigPatch.parse(x));
    }
    assertEquals(
        false,
        DeviceDtos.ConfigPatch.parse(
                n("{\"expected_version\":1,\"settings\":{\"vibration_enabled\":false}}"))
            .settings()
            .get("vibration_enabled"));
  }

  @Test
  void requiredNullableFieldsRemainRequired() throws Exception {
    assertThrows(
        ApiException.class,
        () -> FeedbackDtos.Create.parse(n("{\"category\":\"BUG\",\"content\":\"x\"}")));
    assertNull(
        FeedbackDtos.Create.parse(
                n("{\"device_id\":null,\"category\":\"BUG\",\"content\":\" x \"}"))
            .device());
  }

  @Test
  void cursorsBindCallerFilterAndRejectTampering() {
    Cursors c = new Cursors(Base64.getEncoder().encodeToString(new byte[32]), m);
    UUID u = UUID.randomUUID(), id = UUID.randomUUID();
    assertNotEquals(
        c.scope(u, "users", Map.of("q", "a, status=ACTIVE")),
        c.scope(u, "users", Map.of("q", "a", "status", "ACTIVE")));
    String scope = c.scope(u, "devices", Map.of("status", "ACTIVE"));
    String token = c.encode(scope, Instant.parse("2026-11-07T00:00:00Z"), id);
    assertEquals(id, c.decode(token, scope).id());
    assertThrows(
        ApiException.class,
        () -> c.decode(token, c.scope(UUID.randomUUID(), "devices", Map.of("status", "ACTIVE"))));
    assertThrows(
        ApiException.class,
        () -> c.decode(token, c.scope(u, "devices", Map.of("status", "DISABLED"))));
    assertThrows(ApiException.class, () -> c.decode("x" + token, scope));
    assertThrows(ApiException.class, () -> c.limit(Map.of("limit", "101")));
  }

  @Test
  void tokenExpiryAndSignature() {
    Instant now = Instant.parse("2026-11-07T00:00:00Z");
    String key = Base64.getEncoder().encodeToString(new byte[32]);
    Tokens t = new Tokens(key, Clock.fixed(now, ZoneOffset.UTC));
    UUID user = UUID.randomUUID(), family = UUID.randomUUID();
    String jwt = t.issue(user, family);
    assertEquals(user.toString(), t.verify(jwt).getSubject());
    assertThrows(
        ApiException.class,
        () -> new Tokens(key, Clock.fixed(now.plusSeconds(900), ZoneOffset.UTC)).verify(jwt));
    assertThrows(ApiException.class, () -> t.verify(jwt.substring(0, jwt.length() - 5) + "wrong"));
    assertThrows(IllegalStateException.class, () -> new Tokens("short", Clock.systemUTC()));
  }
}
