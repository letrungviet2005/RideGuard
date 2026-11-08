package com.rideguard.feedback;

import com.rideguard.auth.Actor;
import com.rideguard.devices.DevicesRepository;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
  private final FeedbackRepository repo;
  private final DevicesRepository devices;
  private final Idempotency idem;
  private final Cursors cursors;
  private final Clock clock;

  public FeedbackService(
      FeedbackRepository r, DevicesRepository d, Idempotency i, Cursors c, Clock k) {
    repo = r;
    devices = d;
    idem = i;
    cursors = c;
    clock = k;
  }

  public record Result(int status, Map<String, Object> body) {}

  private Map<String, Object> view(Map<String, Object> r, boolean admin) {
    return Json.pick(r, FeedbackRepository.FIELDS + (admin ? ",user_id,binding_id" : ""));
  }

  public Object get(Actor a, UUID id) {
    var r = repo.get(id);
    if (r == null || !r.get("user_id").equals(a.userId())) throw ApiException.missing();
    return view(r, false);
  }

  @Transactional
  public Result create(Actor a, UUID key, FeedbackDtos.Create d) {
    String route = "/api/v1/feedbacks",
        hash =
            idem.hash(
                Json.obj(
                    "device_id", d.device(), "category", d.category(), "content", d.content()));
    var replay = idem.replay(a.userId(), route, key, hash);
    if (replay != null) return new Result(200, replay);
    UUID binding =
        d.device() == null ? null : (UUID) devices.owned(a.userId(), d.device()).get("id");
    UUID id = UUID.randomUUID();
    repo.create(id, a.userId(), binding, d, clock.instant());
    var body = view(repo.get(id), false);
    idem.save(a.userId(), route, key, hash, "FEEDBACK", id, 201, body);
    return new Result(201, body);
  }

  @Transactional
  public Object patch(Actor a, UUID id, FeedbackDtos.Patch d) {
    if (repo.lock(id) == null) throw ApiException.missing();
    repo.patch(id, d.status(), d.response(), a.userId(), clock.instant());
    return view(repo.get(id), true);
  }

  public Object list(Actor a, Map<String, String> q, boolean admin) {
    var f =
        cursors.filters(
            q, admin ? "status,category,device_id,user_id" : "status,category,device_id");
    Listing.enums(f, "status", "OPEN,IN_PROGRESS,RESOLVED");
    Listing.enums(f, "category", "BUG,SUGGESTION,OTHER");
    String scope = cursors.scope(a.userId(), admin ? "admin-feedback" : "feedback", f);
    int n = cursors.limit(q);
    return cursors.page(
        repo.list(a.userId(), f, cursors.decode(q.get("cursor"), scope), n, admin), n, scope);
  }
}
