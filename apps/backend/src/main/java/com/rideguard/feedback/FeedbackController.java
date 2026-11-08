package com.rideguard.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.auth.Actor;
import com.rideguard.shared.ResponseViews;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class FeedbackController {
  private final ResponseViews views;
  private final FeedbackService service;

  public FeedbackController(FeedbackService s, ResponseViews views) {
    this.views = views;
    service = s;
  }

  @PostMapping("/api/v1/feedbacks")
  ResponseEntity<?> create(
      @AuthenticationPrincipal Actor a,
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestBody JsonNode n) {
    var r = service.create(a, key, FeedbackDtos.Create.parse(n));
    return ResponseEntity.status(r.status()).body(views.one(r.body(), Responses.Feedback.class));
  }

  @GetMapping("/api/v1/feedbacks")
  Object list(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.list(a, q, false), Responses.Feedback.class);
  }

  @GetMapping("/api/v1/feedbacks/{id}")
  Object get(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
    return views.one(service.get(a, id), Responses.Feedback.class);
  }

  @GetMapping("/api/v1/admin/feedbacks")
  Object admin(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.list(a, q, true), Responses.AdminFeedback.class);
  }

  @PatchMapping("/api/v1/admin/feedbacks/{id}")
  Object patch(@AuthenticationPrincipal Actor a, @PathVariable UUID id, @RequestBody JsonNode n) {
    return views.one(
        service.patch(a, id, FeedbackDtos.Patch.parse(n)), Responses.AdminFeedback.class);
  }
}
