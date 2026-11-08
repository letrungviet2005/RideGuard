package com.rideguard.shared;

import java.util.*;

public final class Listing {
  public final StringBuilder where = new StringBuilder(" WHERE true");
  public final List<Object> args = new ArrayList<>();

  public void eq(String column, Object value) {
    if (value != null) {
      where.append(" AND ").append(column).append("=?");
      args.add(value);
    }
  }

  public void condition(String clause, Object... values) {
    where.append(" AND ").append(clause);
    args.addAll(Arrays.asList(values));
  }

  public String end(String time, String id, Cursors.Position pos, int limit) {
    if (pos != null) condition("(" + time + "," + id + ") < (?,?)", pos.time(), pos.id());
    args.add(limit + 1);
    return where + " ORDER BY " + time + " DESC," + id + " DESC LIMIT ?";
  }

  public static void enums(Map<String, String> f, String name, String values) {
    if (f.containsKey(name) && !Set.of(values.split(",")).contains(f.get(name)))
      throw ApiException.bad();
  }

  public static UUID uuid(Map<String, String> f, String name) {
    return f.containsKey(name) ? Json.uuid(f.get(name)) : null;
  }

  public static String search(Map<String, String> f) {
    String q = f.get("q");
    if (q == null) return null;
    q = q.strip();
    if (q.isEmpty() || q.length() > 100) throw ApiException.bad();
    return q;
  }
}
