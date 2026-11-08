package com.rideguard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository extends Sql {
  public AuthRepository(JdbcTemplate j, ObjectMapper m) {
    super(j, m);
  }

  public Map<String, Object> email(String email) {
    return one("SELECT * FROM rideguard.users WHERE email=?", email);
  }

  public Map<String, Object> user(UUID id, boolean lock) {
    return one("SELECT * FROM rideguard.users WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public void register(UUID id, String email, String name, String hash, String role, Instant now) {
    update(
        "INSERT INTO rideguard.users(id,email,full_name,password_hash,role,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?)",
        id,
        email,
        name,
        hash,
        role,
        now,
        now);
  }

  public Map<String, Object> token(String hash) {
    return one("SELECT * FROM rideguard.auth_sessions WHERE refresh_token_hash=?", hash);
  }

  public Map<String, Object> session(UUID id) {
    return one("SELECT * FROM rideguard.auth_sessions WHERE id=?", id);
  }

  public boolean active(UUID user, UUID family, Instant now) {
    return one(
            "SELECT id FROM rideguard.auth_sessions WHERE family_id=? AND user_id=? AND consumed_at"
                + " IS NULL AND revoked_at IS NULL AND expires_at>?",
            family,
            user,
            now)
        != null;
  }

  public void session(
      UUID id,
      UUID family,
      UUID user,
      int generation,
      String hash,
      String type,
      String name,
      Instant now,
      Instant expiry) {
    update(
        "INSERT INTO"
            + " rideguard.auth_sessions(id,family_id,user_id,generation,refresh_token_hash,client_type,client_name,created_at,last_seen_at,expires_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
        id,
        family,
        user,
        generation,
        hash,
        type,
        name,
        now,
        now,
        expiry);
  }

  public void consume(UUID id, UUID next, Instant now) {
    update(
        "UPDATE rideguard.auth_sessions SET consumed_at=?,last_seen_at=?,replaced_by_session_id=?"
            + " WHERE id=?",
        now,
        now,
        next,
        id);
  }

  public void revoke(UUID user, UUID family, Instant now) {
    update(
        "UPDATE rideguard.auth_sessions SET revoked_at=COALESCE(revoked_at,?) WHERE user_id=? AND"
            + " (?::uuid IS NULL OR family_id=?)",
        now,
        user,
        family,
        family);
  }

  public void revokeOthers(UUID user, UUID family, Instant now) {
    update(
        "UPDATE rideguard.auth_sessions SET revoked_at=COALESCE(revoked_at,?) WHERE user_id=? AND"
            + " family_id<>?",
        now,
        user,
        family);
  }

  public void password(UUID id, String hash, Instant now) {
    update("UPDATE rideguard.users SET password_hash=?,updated_at=? WHERE id=?", hash, now, id);
  }

  public void reset(UUID id, UUID user, String hash, Instant now) {
    update(
        "UPDATE rideguard.password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",
        now,
        user);
    update(
        "INSERT INTO rideguard.password_reset_tokens(id,user_id,token_hash,created_at,expires_at)"
            + " VALUES(?,?,?,?,?)",
        id,
        user,
        hash,
        now,
        now.plusSeconds(900));
  }

  public Map<String, Object> reset(String hash) {
    return one("SELECT * FROM rideguard.password_reset_tokens WHERE token_hash=?", hash);
  }

  public boolean useReset(UUID id, Instant now) {
    return update(
            "UPDATE rideguard.password_reset_tokens SET used_at=? WHERE id=? AND used_at IS NULL"
                + " AND expires_at>?",
            now,
            id,
            now)
        == 1;
  }
}
