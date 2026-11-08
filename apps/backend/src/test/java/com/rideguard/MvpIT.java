package com.rideguard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import com.rideguard.auth.*;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
class MvpIT extends PostgresIT {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired AuthRepository auth;
  @Autowired PasswordEncoder passwords;
  @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
  @MockitoBean ResetMail mail;
  private static final String PASSWORD = "integration-only-passphrase";

  record Account(UUID id, String email, String access, String refresh) {}

  JsonNode call(MockHttpServletRequestBuilder req, int expected) throws Exception {
    var response = mvc.perform(req).andReturn().getResponse();
    assertEquals(
        expected,
        response.getStatus(),
        "Unexpected HTTP status; inspect code with test debugger without logging tokens");
    String body = response.getContentAsString();
    return body.isEmpty() ? mapper.nullNode() : mapper.readTree(body);
  }

  MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder r, Object data) {
    try {
      return r.contentType("application/json").content(mapper.writeValueAsBytes(data));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder r, Account a) {
    return r.header("Authorization", "Bearer " + a.access());
  }

  Account account(boolean admin) throws Exception {
    String email = UUID.randomUUID() + "@test.invalid";
    UUID id;
    if (admin) {
      id = UUID.randomUUID();
      auth.register(id, email, "Admin", passwords.encode(PASSWORD), "ADMIN", Instant.now());
    } else {
      id =
          UUID.fromString(
              call(
                      body(
                          post("/api/v1/auth/register"),
                          Json.obj("email", email, "full_name", "Reader", "password", PASSWORD)),
                      201)
                  .path("id")
                  .asText());
    }
    JsonNode tokens =
        call(
            body(
                post("/api/v1/auth/login").header("X-Client-Type", "mobile"),
                Json.obj("email", email, "password", PASSWORD)),
            200);
    return new Account(
        id, email, tokens.path("access_token").asText(), tokens.path("refresh_token").asText());
  }

  JsonNode device(Account admin) throws Exception {
    return call(
        body(
            bearer(post("/api/v1/admin/devices"), admin),
            Json.obj("serial_number", "RG-" + UUID.randomUUID(), "model", "Prototype")),
        201);
  }

  JsonNode bind(Account a, String serial, String key, int status) throws Exception {
    return call(
        body(
            bearer(post("/api/v1/devices/bind"), a).header("Idempotency-Key", key),
            Json.obj("serial_number", serial)),
        status);
  }

  void unbind(Account a, String id, String binding) throws Exception {
    call(
        bearer(
            delete("/api/v1/devices/" + id + "/binding").param("expected_binding_id", binding), a),
        204);
  }

  @Test
  void migrationAndConstraints() {
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT count(*) FROM public.flyway_schema_history WHERE success AND version IN"
                + " ('1','2')",
            Integer.class));
    assertEquals(
        10,
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema='rideguard' AND"
                + " table_type='BASE TABLE'",
            Integer.class));
    assertNotNull(
        jdbc.queryForObject(
            "SELECT indexname FROM pg_indexes WHERE schemaname='rideguard' AND"
                + " indexname='device_active_binding'",
            String.class));
  }

  @Test
  void registrationProfileAndValidation() throws Exception {
    Account a = account(false);
    call(
        body(
            post("/api/v1/auth/register"),
            Json.obj(
                "email", a.email().toUpperCase(), "full_name", "Duplicate", "password", PASSWORD)),
        409);
    call(
        body(
            post("/api/v1/auth/register"),
            Json.obj(
                "email",
                UUID.randomUUID() + "@test.invalid",
                "full_name",
                "Bad",
                "password",
                PASSWORD,
                "role",
                "ADMIN")),
        400);
    call(body(bearer(patch("/api/v1/users/me"), a), Json.obj("full_name", " Renamed ")), 200);
    assertEquals(
        "Renamed", call(bearer(get("/api/v1/users/me"), a), 200).path("full_name").asText());
    call(bearer(get("/api/v1/admin/users"), a), 403);
  }

  @Test
  void refreshReplayRevokesFamilyAndLogoutRetries() throws Exception {
    Account a = account(false);
    var next =
        call(
            body(
                post("/api/v1/auth/refresh").header("X-Client-Type", "mobile"),
                Json.obj("refresh_token", a.refresh())),
            200);
    call(
        body(
            post("/api/v1/auth/refresh").header("X-Client-Type", "mobile"),
            Json.obj("refresh_token", a.refresh())),
        401);
    call(
        get("/api/v1/users/me")
            .header("Authorization", "Bearer " + next.path("access_token").asText()),
        401);
    for (int i = 0; i < 2; i++)
      call(
          body(
              post("/api/v1/auth/logout").header("X-Client-Type", "mobile"),
              Json.obj("refresh_token", a.refresh())),
          204);
  }

  @Test
  void concurrentRefreshOnlyOneRotation() throws Exception {
    Account a = account(false);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<Integer> work =
          () -> {
            start.await();
            return mvc.perform(
                    body(
                        post("/api/v1/auth/refresh").header("X-Client-Type", "mobile"),
                        Json.obj("refresh_token", a.refresh())))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var x = pool.submit(work);
      var y = pool.submit(work);
      start.countDown();
      assertEquals(
          Set.of(200, 401), Set.of(x.get(20, TimeUnit.SECONDS), y.get(20, TimeUnit.SECONDS)));
    }
    call(bearer(get("/api/v1/users/me"), a), 401);
  }

  @Test
  void sessionsLogicalAndChangePasswordRevokesOthers() throws Exception {
    Account a = account(false);
    var other =
        call(
            body(
                post("/api/v1/auth/login").header("X-Client-Type", "mobile"),
                Json.obj("email", a.email(), "password", PASSWORD)),
            200);
    call(
        body(
            post("/api/v1/auth/refresh").header("X-Client-Type", "mobile"),
            Json.obj("refresh_token", a.refresh())),
        200);
    var sessions = call(bearer(get("/api/v1/users/me/sessions"), a), 200);
    assertEquals(2, sessions.path("items").size());
    assertFalse(sessions.toString().contains("token_hash"));
    call(
        body(
            bearer(post("/api/v1/users/me/change-password"), a),
            Json.obj("current_password", PASSWORD, "new_password", PASSWORD + "-new")),
        204);
    call(
        get("/api/v1/users/me")
            .header("Authorization", "Bearer " + other.path("access_token").asText()),
        401);
    call(bearer(get("/api/v1/users/me"), a), 200);
    String current = sessions.path("items").get(0).path("id").asText();
    for (JsonNode s : sessions.path("items"))
      if (s.path("is_current").asBoolean()) current = s.path("id").asText();
    call(bearer(delete("/api/v1/users/me/sessions/" + current), a), 204);
    call(bearer(get("/api/v1/users/me"), a), 401);
  }

  @Test
  void lockUnlockDoesNotRestoreSessionAndSelfLockProtected() throws Exception {
    Account admin = account(true), user = account(false);
    call(
        body(
            bearer(patch("/api/v1/admin/users/" + admin.id() + "/status"), admin),
            Json.obj("status", "LOCKED")),
        409);
    call(
        body(
            bearer(patch("/api/v1/admin/users/" + user.id() + "/status"), admin),
            Json.obj("status", "LOCKED")),
        200);
    call(bearer(get("/api/v1/users/me"), user), 401);
    call(
        body(
            bearer(patch("/api/v1/admin/users/" + user.id() + "/status"), admin),
            Json.obj("status", "ACTIVE")),
        200);
    call(bearer(get("/api/v1/users/me"), user), 401);
  }

  @Test
  void resetOneTimeRevokesSessionsWithoutTokenResponse() throws Exception {
    Account a = account(false);
    var response =
        call(body(post("/api/v1/auth/forgot-password"), Json.obj("email", a.email())), 202);
    assertFalse(response.has("reset_token"));
    var captor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(mail, timeout(10000)).send(eq(a.email()), captor.capture());
    String raw = captor.getValue();
    // The fake adapter is invoked inside the transaction; wait for its commit, not merely send().
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (jdbc.queryForObject(
                "SELECT count(*) FROM rideguard.password_reset_tokens WHERE token_hash=?",
                Integer.class,
                Crypto.hash(raw))
            == 0
        && System.nanoTime() < deadline) {
      Thread.sleep(20);
    }
    call(
        body(
            post("/api/v1/auth/reset-password"),
            Json.obj("reset_token", raw, "new_password", PASSWORD + "-reset")),
        204);
    call(bearer(get("/api/v1/users/me"), a), 401);
    call(
        body(
            post("/api/v1/auth/reset-password"),
            Json.obj("reset_token", raw, "new_password", PASSWORD + "-again")),
        400);
  }

  @Test
  void bindRacePrivacyAndStaleUnbind() throws Exception {
    Account admin = account(true), a = account(false), b = account(false);
    JsonNode d = device(admin);
    String id = d.path("id").asText(), serial = d.path("serial_number").asText();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      List<Future<Integer>> futures = new ArrayList<>();
      for (Account owner : List.of(a, b))
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return mvc.perform(
                          body(
                              bearer(post("/api/v1/devices/bind"), owner)
                                  .header("Idempotency-Key", UUID.randomUUID()),
                              Json.obj("serial_number", serial)))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      start.countDown();
      Set<Integer> codes = new HashSet<>();
      for (var f : futures) codes.add(f.get(20, TimeUnit.SECONDS));
      assertEquals(Set.of(201, 409), codes);
    }
    UUID owner =
        jdbc.queryForObject(
            "SELECT user_id FROM rideguard.device_bindings WHERE device_id=? AND unbound_at IS"
                + " NULL",
            UUID.class,
            UUID.fromString(id));
    Account own = owner.equals(a.id()) ? a : b, foreign = owner.equals(a.id()) ? b : a;
    call(bearer(get("/api/v1/devices/" + id), foreign), 404);
    var original = call(bearer(get("/api/v1/devices/" + id), own), 200);
    String first = original.path("binding_id").asText();
    unbind(own, id, first);
    var second = bind(own, serial, UUID.randomUUID().toString(), 201);
    unbind(own, id, first);
    assertEquals(
        second.path("binding_id").asText(),
        call(bearer(get("/api/v1/devices/" + id), own), 200).path("binding_id").asText());
    call(bearer(delete("/api/v1/devices/" + id + "/binding"), own), 400);
    call(
        bearer(
            delete("/api/v1/devices/" + id + "/binding").param("expected_binding_id", first),
            foreign),
        404);
  }

  @Test
  void idempotencyConflictExpiryAndClosedBinding() throws Exception {
    Account admin = account(true), a = account(false);
    JsonNode d = device(admin);
    String serial = d.path("serial_number").asText(), key = UUID.randomUUID().toString();
    var first = bind(a, serial, key, 201);
    assertEquals(first, bind(a, serial, key, 200));
    call(
        body(
            bearer(post("/api/v1/devices/bind"), a).header("Idempotency-Key", key),
            Json.obj("serial_number", serial, "nickname", "changed")),
        409);
    jdbc.update(
        "UPDATE rideguard.idempotency_records SET created_at=created_at-interval '8"
            + " days',completed_at=completed_at-interval '8"
            + " days',response_expires_at=response_expires_at-interval '8 days' WHERE user_id=? AND"
            + " idempotency_key=?",
        a.id(),
        UUID.fromString(key));
    assertEquals("IDEMPOTENCY_KEY_EXPIRED", bind(a, serial, key, 409).path("code").asText());
    unbind(a, d.path("id").asText(), first.path("binding_id").asText());
    assertEquals("BINDING_NO_LONGER_ACTIVE", bind(a, serial, key, 409).path("code").asText());
  }

  @Test
  void configCasIssuedAckAndReportOrdering() throws Exception {
    Account admin = account(true), a = account(false);
    var d = device(admin);
    String id = d.path("id").asText(),
        binding =
            bind(a, d.path("serial_number").asText(), UUID.randomUUID().toString(), 201)
                .path("binding_id")
                .asText(),
        path = "/api/v1/devices/" + id;
    var initial = call(bearer(get(path + "/config"), a), 200);
    assertTrue(initial.path("reported_applied_version").isNull());
    call(
        body(
            bearer(patch(path + "/config"), a),
            Json.obj("expected_version", 1, "settings", Json.obj("vibration_enabled", false))),
        200);
    call(
        body(
            bearer(patch(path + "/config"), a),
            Json.obj("expected_version", 1, "settings", Json.obj("display_brightness", 10))),
        409);
    call(
        body(
            bearer(post(path + "/config/ack"), a),
            Json.obj("binding_id", binding, "reported_applied_version", 3)),
        409);
    for (int v : new int[] {2, 1, 2})
      assertEquals(
          2,
          call(
                  body(
                      bearer(post(path + "/config/ack"), a),
                      Json.obj("binding_id", binding, "reported_applied_version", v)),
                  200)
              .path("reported_applied_version")
              .asInt());
    call(
        body(
            bearer(post(path + "/config/ack"), a),
            Json.obj("binding_id", UUID.randomUUID(), "reported_applied_version", 1)),
        404);
    Instant observed = Instant.now().minusSeconds(5);
    var report =
        Json.obj(
            "binding_id",
            binding,
            "observed_at",
            observed.toString(),
            "firmware_version",
            null,
            "radar_status",
            "UNKNOWN",
            "imu_status",
            "UNKNOWN",
            "display_status",
            "OK",
            "vibration_status",
            "UNAVAILABLE",
            "battery_percent",
            null);
    assertTrue(
        call(body(bearer(post(path + "/reports"), a), report), 200).path("accepted").asBoolean());
    assertFalse(
        call(body(bearer(post(path + "/reports"), a), report), 200).path("accepted").asBoolean());
    report.put("observed_at", observed.minusSeconds(1).toString());
    assertFalse(
        call(body(bearer(post(path + "/reports"), a), report), 200).path("accepted").asBoolean());
    report.put("observed_at", observed.plusSeconds(1000).toString());
    call(body(bearer(post(path + "/reports"), a), report), 400);
    assertNotNull(call(bearer(get(path), a), 200).get("latest_report"));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "UPDATE rideguard.device_configs SET reported_applied_version=900,reported_at=now()"
                    + " WHERE binding_id=?",
                UUID.fromString(binding)));
    // An orphan issued version must fail its real PostgreSQL foreign key.
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO"
                    + " rideguard.device_config_versions(binding_id,version,schema_version,settings,issued_at)"
                    + " VALUES(?,1,1,'{\"display_brightness\":70,\"vibration_enabled\":true}'::jsonb,now())",
                UUID.randomUUID()));
    // A deferred snapshot constraint fails at commit and rolls back both writes.
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            transactions.executeWithoutResult(
                transaction -> {
                  jdbc.update(
                      "INSERT INTO"
                          + " rideguard.device_config_versions(binding_id,version,schema_version,settings,issued_at)"
                          + " VALUES(?,3,1,'{\"display_brightness\":20,\"vibration_enabled\":false}'::jsonb,now())",
                      UUID.fromString(binding));
                  jdbc.update(
                      "UPDATE rideguard.device_configs SET desired_version=3 WHERE binding_id=?",
                      UUID.fromString(binding));
                }));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT count(*) FROM rideguard.device_config_versions WHERE binding_id=? AND"
                + " version=3",
            Integer.class,
            UUID.fromString(binding)));
    assertEquals(
        2L,
        jdbc.queryForObject(
            "SELECT desired_version FROM rideguard.device_configs WHERE binding_id=?",
            Long.class,
            UUID.fromString(binding)));
  }

  @Test
  void feedbackRetainsAuthorAndAdminDoesNotEditContent() throws Exception {
    Account admin = account(true), a = account(false), b = account(false);
    var d = device(admin);
    String device = d.path("id").asText(),
        binding =
            bind(a, d.path("serial_number").asText(), UUID.randomUUID().toString(), 201)
                .path("binding_id")
                .asText(),
        key = UUID.randomUUID().toString();
    var req = Json.obj("device_id", device, "category", "BUG", "content", "Original report");
    var feedback =
        call(body(bearer(post("/api/v1/feedbacks"), a).header("Idempotency-Key", key), req), 201);
    String id = feedback.path("id").asText();
    unbind(a, device, binding);
    bind(b, d.path("serial_number").asText(), UUID.randomUUID().toString(), 201);
    call(bearer(get("/api/v1/feedbacks/" + id), a), 200);
    call(bearer(get("/api/v1/feedbacks/" + id), b), 404);
    assertEquals(
        feedback,
        call(body(bearer(post("/api/v1/feedbacks"), a).header("Idempotency-Key", key), req), 200));
    call(
        body(
            bearer(patch("/api/v1/admin/feedbacks/" + id), admin),
            Json.obj("content", "Overwrite")),
        400);
    var changed =
        call(
            body(
                bearer(patch("/api/v1/admin/feedbacks/" + id), admin),
                Json.obj("status", "RESOLVED", "admin_response", "Reviewed")),
            200);
    assertEquals("Original report", changed.path("content").asText());
    call(bearer(get("/api/v1/feedbacks").param("category", "BUG"), a), 200);
    call(bearer(get("/api/v1/admin/feedbacks").param("user_id", a.id().toString()), admin), 200);
  }

  @Test
  void keysetPaginationAndTerminalRegistry() throws Exception {
    Account admin = account(true), a = account(false);
    for (int i = 0; i < 3; i++) {
      var d = device(admin);
      bind(a, d.path("serial_number").asText(), UUID.randomUUID().toString(), 201);
    }
    var first = call(bearer(get("/api/v1/devices").param("limit", "1"), a), 200);
    var second =
        call(
            bearer(
                get("/api/v1/devices")
                    .param("limit", "1")
                    .param("cursor", first.path("next_cursor").asText()),
                a),
            200);
    assertNotEquals(first.path("items").get(0).path("id"), second.path("items").get(0).path("id"));
    call(
        bearer(
            get("/api/v1/devices")
                .param("cursor", first.path("next_cursor").asText())
                .param("status", "ACTIVE"),
            a),
        400);
    call(bearer(get("/api/v1/device-bindings"), a), 200);
    call(bearer(get("/api/v1/admin/devices"), admin), 200);
    call(bearer(get("/api/v1/admin/users"), admin), 200);
    String id = first.path("items").get(0).path("id").asText();
    call(body(bearer(patch("/api/v1/devices/" + id), a), Json.obj("nickname", "Mirror")), 200);
    call(
        body(
            bearer(patch("/api/v1/admin/devices/" + id + "/status"), admin),
            Json.obj("status", "RETIRED")),
        200);
    call(
        body(
            bearer(patch("/api/v1/admin/devices/" + id + "/status"), admin),
            Json.obj("status", "ACTIVE")),
        409);
  }
}
