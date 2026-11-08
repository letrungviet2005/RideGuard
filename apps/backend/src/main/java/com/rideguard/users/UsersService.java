package com.rideguard.users;

import com.rideguard.auth.*;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsersService {
  private final UsersRepository repo;
  private final AuthRepository auth;
  private final Cursors cursors;
  private final Clock clock;

  public UsersService(UsersRepository r, AuthRepository a, Cursors c, Clock k) {
    repo = r;
    auth = a;
    cursors = c;
    clock = k;
  }

  public Map<String, Object> me(Actor a) {
    return AuthService.view(auth.user(a.userId(), false));
  }

  @Transactional
  public Map<String, Object> profile(Actor a, String name) {
    auth.user(a.userId(), true);
    repo.profile(a.userId(), name, clock.instant());
    return me(a);
  }

  public Object list(Actor a, Map<String, String> q) {
    var f = cursors.filters(q, "q,status,role");
    Listing.enums(f, "status", "ACTIVE,LOCKED");
    Listing.enums(f, "role", "USER,ADMIN");
    String scope = cursors.scope(a.userId(), "users", f);
    int n = cursors.limit(q);
    return cursors.page(repo.users(f, cursors.decode(q.get("cursor"), scope), n), n, scope);
  }

  public Object sessions(Actor a, Map<String, String> q) {
    var f = cursors.filters(q, "");
    String scope = cursors.scope(a.userId(), "sessions:" + a.familyId(), f);
    int n = cursors.limit(q);
    return cursors.page(
        repo.sessions(
            a.userId(), a.familyId(), clock.instant(), cursors.decode(q.get("cursor"), scope), n),
        n,
        scope);
  }

  @Transactional
  public void revoke(Actor a, UUID family) {
    auth.user(a.userId(), true);
    var r = auth.session(family);
    if (r == null || !r.get("user_id").equals(a.userId()) || !r.get("family_id").equals(family))
      throw ApiException.missing();
    auth.revoke(a.userId(), family, clock.instant());
  }

  @Transactional
  public Object status(Actor a, UUID id, String status) {
    repo.adminLock();
    var u = auth.user(id, true);
    if (u == null) throw ApiException.missing();
    if (status.equals("LOCKED")) {
      if (id.equals(a.userId())) throw new ApiException(409, "ADMIN_SELF_LOCK");
      if (u.get("role").equals("ADMIN")
          && u.get("status").equals("ACTIVE")
          && repo.activeAdmins() <= 1) throw new ApiException(409, "LAST_ADMIN");
      auth.revoke(id, null, clock.instant());
    }
    repo.status(id, status, clock.instant());
    return AuthService.view(auth.user(id, false));
  }
}
