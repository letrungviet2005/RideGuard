package com.rideguard.devices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DevicesRepository extends Sql {
  public DevicesRepository(JdbcTemplate j, ObjectMapper m) {
    super(j, m);
  }

  public Map<String, Object> device(UUID id) {
    return one("SELECT * FROM rideguard.devices WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> serial(String serial) {
    return one("SELECT * FROM rideguard.devices WHERE serial_number=? FOR UPDATE", serial);
  }

  public Map<String, Object> active(UUID id) {
    return one(
        "SELECT * FROM rideguard.device_bindings WHERE device_id=? AND unbound_at IS NULL FOR"
            + " UPDATE",
        id);
  }

  public Map<String, Object> binding(UUID id) {
    return one("SELECT * FROM rideguard.device_bindings WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> owned(UUID user, UUID device) {
    var d = device(device);
    if (d == null) throw ApiException.missing();
    var b = active(device);
    if (b == null || !b.get("user_id").equals(user)) throw ApiException.missing();
    return b;
  }

  public Map<String, Object> view(UUID id, UUID user) {
    return one(
        "SELECT d.id,b.id AS"
            + " binding_id,d.serial_number,b.nickname,d.model,d.hardware_revision,d.status FROM"
            + " rideguard.devices d JOIN rideguard.device_bindings b ON b.device_id=d.id WHERE"
            + " d.id=? AND b.user_id=? AND b.unbound_at IS NULL",
        id,
        user);
  }

  public Map<String, Object> latest(UUID binding) {
    return one(
        "SELECT"
            + " binding_id,observed_at,received_at,firmware_version,radar_status,imu_status,display_status,vibration_status,battery_percent"
            + " FROM rideguard.device_reports WHERE binding_id=? AND accepted ORDER BY observed_at"
            + " DESC,id DESC LIMIT 1",
        binding);
  }

  public void bind(UUID id, UUID device, UUID user, String nickname, Instant now) {
    update(
        "INSERT INTO rideguard.device_bindings(id,device_id,user_id,nickname,bound_at)"
            + " VALUES(?,?,?,?,?)",
        id,
        device,
        user,
        nickname,
        now);
    var settings = Json.obj("display_brightness", 70, "vibration_enabled", true);
    issue(id, 1, settings, now);
    update(
        "INSERT INTO"
            + " rideguard.device_configs(binding_id,schema_version,desired_version,settings,updated_at)"
            + " VALUES(?,1,1,?::jsonb,?)",
        id,
        json(settings),
        now);
  }

  public void unbind(UUID id, Instant now) {
    update(
        "UPDATE rideguard.device_bindings SET unbound_at=? WHERE id=? AND unbound_at IS NULL",
        now,
        id);
  }

  public void nickname(UUID id, String name) {
    update("UPDATE rideguard.device_bindings SET nickname=? WHERE id=?", name, id);
  }

  public Map<String, Object> config(UUID id) {
    return one(
        "SELECT binding_id,schema_version,desired_version,reported_applied_version,settings FROM"
            + " rideguard.device_configs WHERE binding_id=? FOR UPDATE",
        id);
  }

  public void issue(UUID id, long v, Object settings, Instant now) {
    update(
        "INSERT INTO"
            + " rideguard.device_config_versions(binding_id,version,schema_version,settings,issued_at)"
            + " VALUES(?,?,1,?::jsonb,?)",
        id,
        v,
        json(settings),
        now);
  }

  public void config(UUID id, long v, Object settings, Instant now) {
    issue(id, v, settings, now);
    update(
        "UPDATE rideguard.device_configs SET desired_version=?,settings=?::jsonb,updated_at=? WHERE"
            + " binding_id=?",
        v,
        json(settings),
        now,
        id);
  }

  public boolean issued(UUID id, long version) {
    return one(
            "SELECT version FROM rideguard.device_config_versions WHERE binding_id=? AND version=?",
            id,
            version)
        != null;
  }

  public void ack(UUID id, long v, Instant now) {
    update(
        "UPDATE rideguard.device_configs SET reported_applied_version=?,reported_at=? WHERE"
            + " binding_id=? AND (reported_applied_version IS NULL OR reported_applied_version<?)",
        v,
        now,
        id,
        v);
  }

  public void report(DeviceDtos.Report r, Instant now, boolean accepted) {
    update(
        "INSERT INTO"
            + " rideguard.device_reports(id,binding_id,observed_at,received_at,firmware_version,radar_status,imu_status,display_status,vibration_status,battery_percent,accepted)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        r.binding(),
        r.observed(),
        now,
        r.firmware(),
        r.radar(),
        r.imu(),
        r.display(),
        r.vibration(),
        r.battery(),
        accepted);
  }

  public void create(UUID id, DeviceDtos.Create d, Instant now) {
    update(
        "INSERT INTO"
            + " rideguard.devices(id,serial_number,model,hardware_revision,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?)",
        id,
        d.serial(),
        d.model(),
        d.revision(),
        now,
        now);
  }

  public Map<String, Object> admin(UUID id) {
    return one(
        "SELECT d.*,(SELECT b.id FROM rideguard.device_bindings b WHERE b.device_id=d.id AND"
            + " b.unbound_at IS NULL) AS active_binding_id FROM rideguard.devices d WHERE d.id=?",
        id);
  }

  public void status(UUID id, String status, Instant now) {
    update("UPDATE rideguard.devices SET status=?,updated_at=? WHERE id=?", status, now, id);
  }

  public List<Map<String, Object>> list(
      UUID user, Map<String, String> f, Cursors.Position p, int n, boolean history) {
    Listing l = new Listing();
    l.eq("b.user_id", user);
    if (history) {
      l.eq("b.device_id", Listing.uuid(f, "device_id"));
      if (f.containsKey("status"))
        l.condition("b.unbound_at IS " + (f.get("status").equals("ACTIVE") ? "NULL" : "NOT NULL"));
    } else {
      l.condition("b.unbound_at IS NULL");
      l.eq("d.status", f.get("status"));
    }
    String end = l.end("b.bound_at", "b.id", p, n);
    String select =
        history
            ? "b.id,b.device_id,d.serial_number,b.nickname,d.model,d.hardware_revision,b.bound_at,b.unbound_at,CASE"
                  + " WHEN b.unbound_at IS NULL THEN 'ACTIVE' ELSE 'UNBOUND' END AS status"
            : "d.id,b.id AS"
                + " binding_id,d.serial_number,b.nickname,d.model,d.hardware_revision,d.status";
    return rows(
        "SELECT "
            + select
            + ",b.bound_at AS _time,b.id AS _id FROM rideguard.device_bindings b JOIN"
            + " rideguard.devices d ON d.id=b.device_id"
            + end,
        l.args.toArray());
  }

  public List<Map<String, Object>> adminList(Map<String, String> f, Cursors.Position p, int n) {
    Listing l = new Listing();
    l.eq("d.status", f.get("status"));
    String q = Listing.search(f);
    if (q != null)
      l.condition(
          "(position(lower(?) in lower(d.serial_number))>0 OR position(lower(?) in"
              + " lower(d.model))>0)",
          q,
          q);
    String end = l.end("d.created_at", "d.id", p, n);
    return rows(
        "SELECT d.*,(SELECT b.id FROM rideguard.device_bindings b WHERE b.device_id=d.id AND"
            + " b.unbound_at IS NULL) AS active_binding_id,d.created_at AS _time,d.id AS _id FROM"
            + " rideguard.devices d"
            + end,
        l.args.toArray());
  }
}
