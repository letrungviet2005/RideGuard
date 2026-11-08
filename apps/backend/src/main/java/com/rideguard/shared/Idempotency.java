package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Idempotency extends Sql {
  private final Clock clock;

  public Idempotency(JdbcTemplate j, ObjectMapper m, Clock c) {
    super(j, m);
    clock = c;
  }

  public String hash(Map<String, Object> payload) {
    return Crypto.hash(json(new TreeMap<>(payload)));
  }

  public Map<String, Object> replay(UUID user, String route, UUID key, String hash) {
    long lock = Long.parseUnsignedLong(Crypto.hash(user + route + key).substring(0, 16), 16);
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, lock)))
      throw new ApiException(409, "IDEMPOTENCY_IN_PROGRESS");
    var r =
        one(
            "SELECT * FROM rideguard.idempotency_records WHERE user_id=? AND method='POST' AND"
                + " route=? AND idempotency_key=?",
            user,
            route,
            key);
    if (r == null) return null;
    if (!hash.equals(r.get("request_hash"))) throw new ApiException(409, "IDEMPOTENCY_CONFLICT");
    if (r.get("binding_id") != null) {
      var initial =
          one("SELECT device_id FROM rideguard.device_bindings WHERE id=?", r.get("binding_id"));
      if (initial == null) throw ApiException.missing();
      one("SELECT id FROM rideguard.devices WHERE id=? FOR UPDATE", initial.get("device_id"));
      var b =
          one(
              "SELECT unbound_at FROM rideguard.device_bindings WHERE id=? AND user_id=? FOR"
                  + " UPDATE",
              r.get("binding_id"),
              user);
      if (b == null || b.get("unbound_at") != null)
        throw new ApiException(409, "BINDING_NO_LONGER_ACTIVE");
    }
    if (r.get("feedback_id") != null
        && one(
                "SELECT id FROM rideguard.feedbacks WHERE id=? AND user_id=?",
                r.get("feedback_id"),
                user)
            == null) throw ApiException.missing();
    if (!((Instant) r.get("response_expires_at")).isAfter(clock.instant())
        || r.get("response_body") == null) throw new ApiException(409, "IDEMPOTENCY_KEY_EXPIRED");
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) r.get("response_body");
    return body;
  }

  public void save(
      UUID user,
      String route,
      UUID key,
      String hash,
      String kind,
      UUID target,
      int status,
      Object body) {
    Instant now = clock.instant();
    update(
        "INSERT INTO"
            + " rideguard.idempotency_records(id,user_id,method,route,idempotency_key,request_hash,resource_type,binding_id,feedback_id,response_status,response_body,created_at,completed_at,response_expires_at,state)"
            + " VALUES(?,?,'POST',?,?,?,?,?,?,?,?::jsonb,?,?,?,'COMPLETED')",
        UUID.randomUUID(),
        user,
        route,
        key,
        hash,
        kind,
        kind.equals("BINDING") ? target : null,
        kind.equals("FEEDBACK") ? target : null,
        status,
        json(body),
        now,
        now,
        now.plus(Duration.ofDays(7)));
  }
}
