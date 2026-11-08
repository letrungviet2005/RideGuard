package com.rideguard.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackRepository extends Sql {
  public static final String FIELDS =
      "id,device_id,category,content,status,admin_response,created_at,responded_at";

  public FeedbackRepository(JdbcTemplate j, ObjectMapper m) {
    super(j, m);
  }

  public Map<String, Object> get(UUID id) {
    return one("SELECT * FROM rideguard.feedbacks WHERE id=?", id);
  }

  public Map<String, Object> lock(UUID id) {
    return one("SELECT * FROM rideguard.feedbacks WHERE id=? FOR UPDATE", id);
  }

  public void create(UUID id, UUID user, UUID binding, FeedbackDtos.Create d, Instant now) {
    update(
        "INSERT INTO"
            + " rideguard.feedbacks(id,user_id,device_id,binding_id,category,content,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        id,
        user,
        d.device(),
        binding,
        d.category(),
        d.content(),
        now,
        now);
  }

  public void patch(UUID id, String status, String response, UUID admin, Instant now) {
    update(
        "UPDATE rideguard.feedbacks SET"
            + " status=COALESCE(?,status),admin_response=COALESCE(?,admin_response),responded_at=CASE"
            + " WHEN ?::text IS NOT NULL AND admin_response IS DISTINCT FROM ? THEN ? ELSE"
            + " responded_at END,responded_by=CASE WHEN ?::text IS NOT NULL AND admin_response IS"
            + " DISTINCT FROM ? THEN ? ELSE responded_by END,updated_at=? WHERE id=?",
        status,
        response,
        response,
        response,
        now,
        response,
        response,
        admin,
        now,
        id);
  }

  public List<Map<String, Object>> list(
      UUID user, Map<String, String> f, Cursors.Position p, int n, boolean admin) {
    Listing l = new Listing();
    l.eq("user_id", admin ? Listing.uuid(f, "user_id") : user);
    l.eq("device_id", Listing.uuid(f, "device_id"));
    l.eq("status", f.get("status"));
    l.eq("category", f.get("category"));
    String end = l.end("created_at", "id", p, n);
    return rows(
        "SELECT "
            + FIELDS
            + (admin ? ",user_id,binding_id" : "")
            + ",created_at AS _time,id AS _id FROM rideguard.feedbacks"
            + end,
        l.args.toArray());
  }
}
