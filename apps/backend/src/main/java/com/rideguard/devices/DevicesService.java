package com.rideguard.devices;

import com.rideguard.auth.Actor;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevicesService {
  private final DevicesRepository repo;
  private final Idempotency idem;
  private final Cursors cursors;
  private final Clock clock;

  public DevicesService(DevicesRepository r, Idempotency i, Cursors c, Clock k) {
    repo = r;
    idem = i;
    cursors = c;
    clock = k;
  }

  public record Result(int status, Map<String, Object> body) {}

  private Map<String, Object> enrich(Map<String, Object> d) {
    if (d == null) throw ApiException.missing();
    d.put("latest_report", repo.latest((UUID) d.get("binding_id")));
    return d;
  }

  @Transactional
  public Object get(Actor a, UUID id) {
    repo.owned(a.userId(), id);
    return enrich(repo.view(id, a.userId()));
  }

  @Transactional
  public Object rename(Actor a, UUID id, String nickname) {
    var b = repo.owned(a.userId(), id);
    repo.nickname((UUID) b.get("id"), nickname);
    return enrich(repo.view(id, a.userId()));
  }

  @Transactional
  public Result bind(Actor a, UUID key, DeviceDtos.Bind d) {
    String route = "/api/v1/devices/bind",
        hash = idem.hash(Json.obj("serial_number", d.serial(), "nickname", d.nickname()));
    var replay = idem.replay(a.userId(), route, key, hash);
    if (replay != null) return new Result(200, replay);
    var device = repo.serial(d.serial());
    if (device == null || !device.get("status").equals("ACTIVE"))
      throw new ApiException(404, "DEVICE_NOT_AVAILABLE");
    UUID id = (UUID) device.get("id");
    var b = repo.active(id);
    int status = 200;
    UUID binding;
    if (b != null) {
      if (!b.get("user_id").equals(a.userId())) throw new ApiException(409, "DEVICE_UNAVAILABLE");
      binding = (UUID) b.get("id");
    } else {
      binding = UUID.randomUUID();
      repo.bind(binding, id, a.userId(), d.nickname(), clock.instant());
      status = 201;
    }
    var body = enrich(repo.view(id, a.userId()));
    idem.save(a.userId(), route, key, hash, "BINDING", binding, status, body);
    return new Result(status, body);
  }

  @Transactional
  public void unbind(Actor a, UUID device, UUID expected) {
    if (repo.device(device) == null) throw ApiException.missing();
    var b = repo.binding(expected);
    if (b == null || !b.get("device_id").equals(device) || !b.get("user_id").equals(a.userId()))
      throw ApiException.missing();
    repo.unbind(expected, clock.instant());
  }

  @Transactional
  public Object config(Actor a, UUID id) {
    var b = repo.owned(a.userId(), id);
    return repo.config((UUID) b.get("id"));
  }

  @Transactional
  public Object patch(Actor a, UUID id, DeviceDtos.ConfigPatch d) {
    var b = repo.owned(a.userId(), id);
    if (!repo.device(id).get("status").equals("ACTIVE"))
      throw new ApiException(409, "DEVICE_NOT_ACTIVE");
    UUID binding = (UUID) b.get("id");
    var c = repo.config(binding);
    long v = ((Number) c.get("desired_version")).longValue();
    if (v != d.expected() || v >= 9007199254740991L)
      throw new ApiException(409, "VERSION_CONFLICT");
    @SuppressWarnings("unchecked")
    Map<String, Object> settings = new LinkedHashMap<>((Map<String, Object>) c.get("settings"));
    settings.putAll(d.settings());
    repo.config(binding, v + 1, settings, clock.instant());
    return repo.config(binding);
  }

  @Transactional
  public Object ack(Actor a, UUID id, UUID binding, long version) {
    var b = repo.owned(a.userId(), id);
    if (!b.get("id").equals(binding)) throw ApiException.missing();
    if (!repo.issued(binding, version)) throw new ApiException(409, "CONFIG_VERSION_NOT_ISSUED");
    repo.ack(binding, version, clock.instant());
    return repo.config(binding);
  }

  @Transactional
  public Object report(Actor a, UUID id, DeviceDtos.Report r) {
    Instant now = clock.instant();
    var b = repo.owned(a.userId(), id);
    if (!b.get("id").equals(r.binding())) throw ApiException.missing();
    if (r.observed().isBefore(now.minus(Duration.ofHours(24)))
        || r.observed().isAfter(now.plusSeconds(300)))
      throw new ApiException(400, "OBSERVED_AT_OUT_OF_RANGE");
    var last = repo.latest(r.binding());
    boolean accepted = last == null || r.observed().isAfter((Instant) last.get("observed_at"));
    repo.report(r, now, accepted);
    return Json.obj("accepted", accepted, "received_at", now);
  }

  @Transactional
  public Object create(DeviceDtos.Create d) {
    UUID id = UUID.randomUUID();
    try {
      repo.create(id, d, clock.instant());
    } catch (DuplicateKeyException e) {
      throw new ApiException(409, "SERIAL_ALREADY_REGISTERED");
    }
    return repo.admin(id);
  }

  @Transactional
  public Object status(UUID id, String status) {
    var d = repo.device(id);
    if (d == null) throw ApiException.missing();
    if (d.get("status").equals("RETIRED") && !status.equals("RETIRED"))
      throw new ApiException(409, "INVALID_STATUS_TRANSITION");
    repo.status(id, status, clock.instant());
    return repo.admin(id);
  }

  @Transactional(readOnly = true)
  public Object list(Actor a, Map<String, String> q, boolean history, boolean admin) {
    var f = cursors.filters(q, admin ? "q,status" : history ? "status,device_id" : "status");
    Listing.enums(f, "status", history ? "ACTIVE,UNBOUND" : "ACTIVE,DISABLED,RETIRED");
    String scope =
        cursors.scope(a.userId(), admin ? "admin-devices" : history ? "bindings" : "devices", f);
    int n = cursors.limit(q);
    var p = cursors.decode(q.get("cursor"), scope);
    var rows = admin ? repo.adminList(f, p, n) : repo.list(a.userId(), f, p, n, history);
    if (!admin && !history) rows.forEach(this::enrich);
    return cursors.page(rows, n, scope);
  }
}
