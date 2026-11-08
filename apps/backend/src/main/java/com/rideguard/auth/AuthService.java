package com.rideguard.auth;

import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthService {
  private final AuthRepository repo;
  private final PasswordEncoder passwords;
  private final Tokens tokens;
  private final Clock clock;
  private final TransactionTemplate tx;
  private final ResetMail mail;
  private final String dummy;
  private final org.springframework.core.task.TaskExecutor resetExecutor;

  public AuthService(
      AuthRepository r,
      PasswordEncoder p,
      Tokens t,
      Clock c,
      TransactionTemplate tx,
      ResetMail mail,
      @org.springframework.beans.factory.annotation.Qualifier("resetExecutor")
          org.springframework.core.task.TaskExecutor resetExecutor) {
    this.resetExecutor = resetExecutor;
    repo = r;
    passwords = p;
    tokens = t;
    clock = c;
    this.tx = tx;
    this.mail = mail;
    dummy = p.encode(Crypto.random());
  }

  public static Map<String, Object> view(Map<String, Object> u) {
    return Json.pick(u, "id,email,full_name,role,status,created_at,updated_at");
  }

  public Map<String, Object> register(AuthDtos.Register d) {
    try {
      return tx.execute(
          s -> {
            UUID id = UUID.randomUUID();
            repo.register(
                id,
                d.email(),
                d.fullName(),
                passwords.encode(d.password()),
                "USER",
                clock.instant());
            return view(repo.user(id, false));
          });
    } catch (DuplicateKeyException e) {
      throw new ApiException(409, "EMAIL_UNAVAILABLE");
    }
  }

  public record Grant(Map<String, Object> body, String refresh, Instant expires) {}

  private Grant grant(
      Map<String, Object> u, UUID family, String refresh, Instant expiry, boolean web) {
    Map<String, Object> b =
        Json.obj(
            "access_token",
            tokens.issue((UUID) u.get("id"), family),
            "token_type",
            "Bearer",
            "expires_in",
            900,
            "user",
            view(u));
    if (!web) b.put("refresh_token", refresh);
    return new Grant(b, refresh, expiry);
  }

  public Grant login(AuthDtos.Login d, String type) {
    Map<String, Object> found = repo.email(d.email());
    if (found == null) {
      passwords.matches(d.password(), dummy);
      throw ApiException.unauthorized();
    }
    return tx.execute(
        s -> {
          var u = repo.user((UUID) found.get("id"), true);
          if (!passwords.matches(d.password(), (String) u.get("password_hash"))
              || !u.get("status").equals("ACTIVE")) throw ApiException.unauthorized();
          if (type.equals("admin_web") && !u.get("role").equals("ADMIN"))
            throw new ApiException(403, "FORBIDDEN");
          UUID f = UUID.randomUUID();
          String raw = Crypto.random();
          Instant now = clock.instant(), expiry = now.plus(Duration.ofDays(30));
          repo.session(
              f, f, (UUID) u.get("id"), 0, Crypto.hash(raw), type, d.clientName(), now, expiry);
          return grant(u, f, raw, expiry, type.equals("admin_web"));
        });
  }

  public Grant refresh(String raw, String type) {
    var found = repo.token(Crypto.hash(raw));
    if (found == null) throw ApiException.unauthorized();
    Grant result =
        tx.execute(
            s -> {
              UUID uid = (UUID) found.get("user_id");
              var u = repo.user(uid, true);
              var old = repo.session((UUID) found.get("id"));
              UUID f = (UUID) old.get("family_id");
              Instant now = clock.instant();
              if (!old.get("client_type").equals(type)) throw ApiException.unauthorized();
              if (old.get("consumed_at") != null) {
                repo.revoke(uid, f, now);
                return null;
              }
              if (old.get("revoked_at") != null
                  || !((Instant) old.get("expires_at")).isAfter(now)
                  || !u.get("status").equals("ACTIVE")) throw ApiException.unauthorized();
              if (type.equals("admin_web") && !u.get("role").equals("ADMIN"))
                throw new ApiException(403, "FORBIDDEN");
              UUID id = UUID.randomUUID();
              String token = Crypto.random();
              repo.consume((UUID) old.get("id"), id, now);
              repo.session(
                  id,
                  f,
                  uid,
                  ((Number) old.get("generation")).intValue() + 1,
                  Crypto.hash(token),
                  type,
                  (String) old.get("client_name"),
                  now,
                  (Instant) old.get("expires_at"));
              return grant(u, f, token, (Instant) old.get("expires_at"), type.equals("admin_web"));
            });
    if (result == null) throw ApiException.unauthorized();
    return result;
  }

  public void logout(String token, String type) {
    var found = repo.token(Crypto.hash(token));
    if (found == null || !found.get("client_type").equals(type)) throw ApiException.unauthorized();
    tx.executeWithoutResult(
        s -> {
          UUID u = (UUID) found.get("user_id");
          repo.user(u, true);
          repo.revoke(u, (UUID) found.get("family_id"), clock.instant());
        });
  }

  public Actor authenticate(String token) {
    var j = tokens.verify(token);
    try {
      UUID u = Json.uuid(j.getSubject()), f = Json.uuid(j.getClaimAsString("sid"));
      var user = repo.user(u, false);
      if (user == null
          || !user.get("status").equals("ACTIVE")
          || !repo.active(u, f, clock.instant())) throw ApiException.unauthorized();
      return new Actor(u, f, (String) user.get("role"));
    } catch (RuntimeException e) {
      if (e instanceof org.springframework.dao.DataAccessException) throw e;
      throw ApiException.unauthorized();
    }
  }

  public void forgot(String email) {
    try {
      resetExecutor.execute(
          () -> {
            try {
              var u = repo.email(email);
              if (u == null) return;
              String raw = Crypto.random();
              tx.executeWithoutResult(
                  s -> {
                    repo.user((UUID) u.get("id"), true);
                    repo.reset(
                        UUID.randomUUID(), (UUID) u.get("id"), Crypto.hash(raw), clock.instant());
                    mail.send(email, raw);
                  });
            } catch (RuntimeException e) {
              org.slf4j.LoggerFactory.getLogger(AuthService.class)
                  .warn(
                      "Password reset delivery failed; inspect SMTP/DB availability (credentials"
                          + " omitted)");
            }
          });
    } catch (org.springframework.core.task.TaskRejectedException e) {
      throw new ApiException(503, "TEMPORARILY_UNAVAILABLE");
    }
  }

  public void reset(String raw, String password) {
    var r = repo.reset(Crypto.hash(raw));
    if (r == null) throw new ApiException(400, "INVALID_RESET_TOKEN");
    tx.executeWithoutResult(
        s -> {
          UUID u = (UUID) r.get("user_id");
          repo.user(u, true);
          Instant now = clock.instant();
          if (!repo.useReset((UUID) r.get("id"), now))
            throw new ApiException(400, "INVALID_RESET_TOKEN");
          repo.password(u, passwords.encode(password), now);
          repo.revoke(u, null, now);
        });
  }

  public void change(Actor a, String current, String next) {
    tx.executeWithoutResult(
        s -> {
          var u = repo.user(a.userId(), true);
          if (!passwords.matches(current, (String) u.get("password_hash")))
            throw new ApiException(400, "CURRENT_PASSWORD_INVALID");
          repo.password(a.userId(), passwords.encode(next), clock.instant());
          repo.revokeOthers(a.userId(), a.familyId(), clock.instant());
        });
  }
}
