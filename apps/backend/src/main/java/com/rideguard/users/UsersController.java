package com.rideguard.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.auth.*;
import com.rideguard.shared.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class UsersController {
  private final ResponseViews views;
  private final UsersService service;
  private final AuthService auth;

  public UsersController(UsersService s, AuthService a, ResponseViews views) {
    this.views = views;
    service = s;
    auth = a;
  }

  @GetMapping("/api/v1/users/me")
  Object me(@AuthenticationPrincipal Actor a) {
    return views.one(service.me(a), Responses.User.class);
  }

  @PatchMapping("/api/v1/users/me")
  Object profile(@AuthenticationPrincipal Actor a, @RequestBody JsonNode n) {
    var d = UserDtos.Profile.parse(n);
    return views.one(service.profile(a, d.fullName()), Responses.User.class);
  }

  @PostMapping("/api/v1/users/me/change-password")
  ResponseEntity<Void> password(@AuthenticationPrincipal Actor a, @RequestBody JsonNode n) {
    var d = UserDtos.ChangePassword.parse(n);
    auth.change(a, d.currentPassword(), d.newPassword());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/v1/users/me/sessions")
  Object sessions(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.sessions(a, q), com.rideguard.auth.Responses.Session.class);
  }

  @DeleteMapping("/api/v1/users/me/sessions/{id}")
  ResponseEntity<Void> revoke(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
    service.revoke(a, id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/v1/admin/users")
  Object users(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.list(a, q), Responses.User.class);
  }

  @PatchMapping("/api/v1/admin/users/{id}/status")
  Object status(@AuthenticationPrincipal Actor a, @PathVariable UUID id, @RequestBody JsonNode n) {
    Json.fields(n, "status", "status");
    return views.one(
        service.status(a, id, Json.choice(n, "status", "ACTIVE,LOCKED")), Responses.User.class);
  }
}
