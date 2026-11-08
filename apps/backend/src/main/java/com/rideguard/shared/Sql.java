package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class Sql {
  protected final JdbcTemplate jdbc;
  protected final ObjectMapper mapper;

  protected Sql(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  protected Object[] args(Object... a) {
    return Arrays.stream(a).map(v -> v instanceof Instant i ? Timestamp.from(i) : v).toArray();
  }

  protected List<Map<String, Object>> rows(String sql, Object... a) {
    return jdbc.query(
        sql,
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            Object v = rs.getObject(i);
            if (v instanceof Timestamp t) v = t.toInstant();
            if (v != null && v.getClass().getName().equals("org.postgresql.util.PGobject")) {
              try {
                v = mapper.readValue(v.toString(), Object.class);
              } catch (Exception e) {
                throw new IllegalStateException("Invalid stored JSON");
              }
            }
            m.put(rs.getMetaData().getColumnLabel(i), v);
          }
          return m;
        },
        args(a));
  }

  protected Map<String, Object> one(String sql, Object... a) {
    var r = rows(sql, a);
    return r.isEmpty() ? null : r.getFirst();
  }

  protected int update(String sql, Object... a) {
    return jdbc.update(sql, args(a));
  }

  public String json(Object v) {
    try {
      return mapper.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot serialize value");
    }
  }
}
