package com.rideguard.shared;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;

public final class Json {
  private Json() {}

  public static Map<String, Object> obj(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  public static Map<String, Object> pick(Map<String, Object> row, String fields) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (String f : fields.split(",")) m.put(f, row.get(f));
    return m;
  }

  public static void fields(JsonNode n, String allowed, String required) {
    if (n == null || !n.isObject()) throw ApiException.bad();
    Set<String> a = allowed.isEmpty() ? Set.of() : Set.of(allowed.split(","));
    n.fieldNames()
        .forEachRemaining(
            k -> {
              if (!a.contains(k)) throw ApiException.bad();
            });
    if (!required.isEmpty())
      for (String k : required.split(",")) if (!n.has(k)) throw ApiException.bad();
  }

  public static String str(JsonNode n, String k, int min, int max, boolean nullable, boolean trim) {
    JsonNode v = n.get(k);
    if (v == null || v.isNull()) {
      if (nullable) return null;
      throw ApiException.bad();
    }
    if (!v.isTextual()) throw ApiException.bad();
    String s = trim ? v.textValue().strip() : v.textValue();
    int len = s.codePointCount(0, s.length());
    if (len < min || len > max) throw ApiException.bad();
    return s;
  }

  public static String choice(JsonNode n, String k, String values) {
    String s = str(n, k, 1, 64, false, false);
    if (!Set.of(values.split(",")).contains(s)) throw ApiException.bad();
    return s;
  }

  public static long number(JsonNode n, String k, long min, long max) {
    JsonNode v = n.get(k);
    if (v == null
        || !v.isIntegralNumber()
        || !v.canConvertToLong()
        || v.longValue() < min
        || v.longValue() > max) throw ApiException.bad();
    return v.longValue();
  }

  public static UUID uuid(JsonNode n, String k, boolean nullable) {
    String s = str(n, k, 1, 36, nullable, false);
    return s == null ? null : uuid(s);
  }

  public static UUID uuid(String s) {
    try {
      UUID u = UUID.fromString(s);
      if (!u.toString().equalsIgnoreCase(s)) throw ApiException.bad();
      return u;
    } catch (IllegalArgumentException e) {
      throw ApiException.bad();
    }
  }

  public static String email(JsonNode n) {
    String s = str(n, "email", 3, 254, false, true).toLowerCase(Locale.ROOT);
    if (s.codePointCount(0, s.length()) > 254 || !s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw ApiException.bad();
    return s;
  }

  public static String serial(JsonNode n) {
    String s = str(n, "serial_number", 1, 128, false, true);
    if (!s.matches("[a-zA-Z0-9][a-zA-Z0-9-]{0,127}")) throw ApiException.bad();
    return s.toUpperCase(Locale.ROOT);
  }

  public static Instant instant(JsonNode n, String k) {
    String s = str(n, k, 1, 64, false, false);
    try {
      if (!s.endsWith("Z")) throw ApiException.bad();
      return Instant.parse(s).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    } catch (java.time.format.DateTimeParseException e) {
      throw ApiException.bad();
    }
  }

  public static String password(JsonNode n, String k) {
    return str(n, k, 12, 128, false, false);
  }
}
