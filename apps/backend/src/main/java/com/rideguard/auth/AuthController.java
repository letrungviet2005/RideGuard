package com.rideguard.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.shared.*;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;
  private final boolean secure;
  private final String sameSite;
  private final Clock clock;

  public AuthController(
      AuthService s,
      @Value("${rideguard.cookie-secure:true}") boolean secure,
      @Value("${rideguard.cookie-same-site:Lax}") String sameSite,
      Clock c) {
    service = s;
    this.secure = secure;
    this.sameSite = sameSite;
    clock = c;
  }

  @PostMapping("/register")
  ResponseEntity<?> register(@RequestBody JsonNode n) {
    return ResponseEntity.status(201).body(service.register(AuthDtos.Register.parse(n)));
  }

  private String type(String t) {
    if (!Set.of("mobile", "admin_web").contains(t)) throw ApiException.bad();
    return t;
  }

  private void cookie(HttpServletResponse res, String raw, long seconds) {
    res.addHeader(
        "Set-Cookie",
        ResponseCookie.from("rg_refresh", raw)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(seconds)
            .build()
            .toString());
  }

  private Map<String, Object> grant(AuthService.Grant g, String t, HttpServletResponse res) {
    res.setHeader("Cache-Control", "no-store");
    if (t.equals("admin_web"))
      cookie(
          res,
          g.refresh(),
          Math.max(0, Duration.between(clock.instant(), g.expires()).toSeconds()));
    return g.body();
  }

  private String credential(JsonNode n, String type, HttpServletRequest r) {
    if (type.equals("mobile")) {
      Json.fields(n, "refresh_token", "refresh_token");
      return Json.str(n, "refresh_token", 32, 4096, false, false);
    }
    Json.fields(n, "", "");
    if (r.getCookies() != null)
      for (Cookie c : r.getCookies()) if (c.getName().equals("rg_refresh")) return c.getValue();
    throw ApiException.unauthorized();
  }

  @PostMapping("/login")
  Object login(
      @RequestBody JsonNode n, @RequestHeader("X-Client-Type") String t, HttpServletResponse r) {
    return grant(service.login(AuthDtos.Login.parse(n), type(t)), t, r);
  }

  @PostMapping("/refresh")
  Object refresh(
      @RequestBody JsonNode n,
      @RequestHeader("X-Client-Type") String t,
      HttpServletRequest q,
      HttpServletResponse r) {
    return grant(service.refresh(credential(n, type(t), q), t), t, r);
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(
      @RequestBody JsonNode n,
      @RequestHeader("X-Client-Type") String t,
      HttpServletRequest q,
      HttpServletResponse r) {
    service.logout(credential(n, type(t), q), t);
    r.setHeader("Cache-Control", "no-store");
    if (t.equals("admin_web")) cookie(r, "", 0);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/forgot-password")
  ResponseEntity<?> forgot(@RequestBody JsonNode n) {
    Json.fields(n, "email", "email");
    service.forgot(Json.email(n));
    return ResponseEntity.accepted()
        .body(Json.obj("message", "If the account exists, reset instructions will be sent."));
  }

  @PostMapping("/reset-password")
  ResponseEntity<Void> reset(@RequestBody JsonNode n) {
    Json.fields(n, "reset_token,new_password", "reset_token,new_password");
    service.reset(
        Json.str(n, "reset_token", 32, 4096, false, false), Json.password(n, "new_password"));
    return ResponseEntity.noContent().build();
  }
}
