package com.rideguard.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsersRepository extends Sql {
  public UsersRepository(JdbcTemplate j, ObjectMapper m) {
    super(j, m);
  }

  public void profile(UUID u, String name, Instant now) {
    update("UPDATE rideguard.users SET full_name=?,updated_at=? WHERE id=?", name, now, u);
  }

  public List<Map<String, Object>> users(Map<String, String> f, Cursors.Position p, int limit) {
    Listing l = new Listing();
    l.eq("status", f.get("status"));
    l.eq("role", f.get("role"));
    String q = Listing.search(f);
    if (q != null)
      l.condition(
          "(position(lower(?) in lower(email))>0 OR position(lower(?) in lower(full_name))>0)",
          q,
          q);
    String end = l.end("created_at", "id", p, limit);
    return rows(
        "SELECT id,email,full_name,role,status,created_at,updated_at,created_at AS _time,id AS _id"
            + " FROM rideguard.users"
            + end,
        l.args.toArray());
  }

  public List<Map<String, Object>> sessions(
      UUID user, UUID current, Instant now, Cursors.Position p, int limit) {
    Listing l = new Listing();
    l.eq("r.user_id", user);
    l.condition("r.generation=0 AND r.revoked_at IS NULL AND r.expires_at>?", now);
    String end = l.end("r.created_at", "r.id", p, limit);
    return rows(
        "SELECT"
            + " r.id,r.client_type,r.client_name,r.created_at,s.last_seen_at,r.expires_at,(r.id=?::uuid)"
            + " AS is_current,r.created_at AS _time,r.id AS _id FROM rideguard.auth_sessions r JOIN"
            + " rideguard.auth_sessions s ON s.family_id=r.id AND s.consumed_at IS NULL AND"
            + " s.revoked_at IS NULL"
            + end,
        prepend(current, l.args));
  }

  private Object[] prepend(Object value, List<Object> list) {
    List<Object> a = new ArrayList<>();
    a.add(value);
    a.addAll(list);
    return a.toArray();
  }

  public void adminLock() {
    jdbc.queryForObject("SELECT pg_advisory_xact_lock(8721301)", Object.class);
  }

  public long activeAdmins() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM rideguard.users WHERE role='ADMIN' AND status='ACTIVE'", Long.class);
  }

  public void status(UUID user, String status, Instant now) {
    update("UPDATE rideguard.users SET status=?,updated_at=? WHERE id=?", status, now, user);
  }
}
