package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Cursors {
  private final byte[] key;
  private final ObjectMapper mapper;

  public Cursors(@Value("${rideguard.cursor-key}") String key, ObjectMapper m) {
    this.key = Crypto.key(key);
    mapper = m;
  }

  public record Position(Instant time, UUID id) {}

  public String scope(UUID caller, String route, Map<String, String> filters) {
    try {
      return Crypto.hash(
          mapper.writeValueAsString(
              Json.obj("caller", caller, "route", route, "filters", new TreeMap<>(filters))));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Cannot encode cursor scope", e);
    }
  }

  public Position decode(String token, String scope) {
    if (token == null) return null;
    try {
      if (token.length() > 2048) throw ApiException.bad();
      String[] s = token.split("\\.");
      if (s.length != 2
          || !MessageDigest.isEqual(Crypto.mac(key, s[0]), Base64.getUrlDecoder().decode(s[1])))
        throw ApiException.bad();
      var n = mapper.readTree(Base64.getUrlDecoder().decode(s[0]));
      if (!scope.equals(n.path("scope").asText())) throw ApiException.bad();
      return new Position(Instant.parse(n.path("time").asText()), Json.uuid(n.path("id").asText()));
    } catch (Exception e) {
      throw new ApiException(400, "INVALID_CURSOR");
    }
  }

  public String encode(String scope, Instant time, UUID id) {
    try {
      String s =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(
                  mapper.writeValueAsBytes(
                      Json.obj("scope", scope, "time", time.toString(), "id", id.toString())));
      return s + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(Crypto.mac(key, s));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public int limit(Map<String, String> q) {
    try {
      int n = Integer.parseInt(q.getOrDefault("limit", "20"));
      if (n < 1 || n > 100) throw ApiException.bad();
      return n;
    } catch (NumberFormatException e) {
      throw ApiException.bad();
    }
  }

  public Map<String, String> filters(Map<String, String> q, String allowed) {
    Set<String> a = new HashSet<>(Arrays.asList(allowed.split(",")));
    a.addAll(List.of("limit", "cursor"));
    if (!a.containsAll(q.keySet())) throw ApiException.bad();
    Map<String, String> f = new TreeMap<>(q);
    f.remove("limit");
    f.remove("cursor");
    return f;
  }

  public Map<String, Object> page(List<Map<String, Object>> rows, int limit, String scope) {
    String next = null;
    if (rows.size() > limit) {
      rows = new ArrayList<>(rows.subList(0, limit));
      var last = rows.getLast();
      next = encode(scope, (Instant) last.get("_time"), (UUID) last.get("_id"));
    }
    for (var r : rows) {
      r.remove("_time");
      r.remove("_id");
    }
    return Json.obj("items", rows, "next_cursor", next);
  }
}
