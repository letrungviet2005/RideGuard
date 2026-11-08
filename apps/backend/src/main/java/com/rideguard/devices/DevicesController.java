package com.rideguard.devices;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.auth.Actor;
import com.rideguard.shared.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class DevicesController {
  private final ResponseViews views;
  private final DevicesService service;

  public DevicesController(DevicesService s, ResponseViews views) {
    this.views = views;
    service = s;
  }

  @GetMapping("/api/v1/devices")
  Object list(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.list(a, q, false, false), Responses.Device.class);
  }

  @GetMapping("/api/v1/device-bindings")
  Object history(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.list(a, q, true, false), Responses.Binding.class);
  }

  @GetMapping("/api/v1/devices/{id}")
  Object get(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
    return views.one(service.get(a, id), Responses.Device.class);
  }

  @PatchMapping("/api/v1/devices/{id}")
  Object rename(@AuthenticationPrincipal Actor a, @PathVariable UUID id, @RequestBody JsonNode n) {
    Json.fields(n, "nickname", "nickname");
    return views.one(
        service.rename(a, id, Json.str(n, "nickname", 1, 60, true, true)), Responses.Device.class);
  }

  @PostMapping("/api/v1/devices/bind")
  ResponseEntity<?> bind(
      @AuthenticationPrincipal Actor a,
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestBody JsonNode n) {
    var result = service.bind(a, key, DeviceDtos.Bind.parse(n));
    return ResponseEntity.status(result.status())
        .body(views.one(result.body(), Responses.Device.class));
  }

  @DeleteMapping("/api/v1/devices/{id}/binding")
  ResponseEntity<Void> unbind(
      @AuthenticationPrincipal Actor a,
      @PathVariable UUID id,
      @RequestParam("expected_binding_id") UUID binding) {
    service.unbind(a, id, binding);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/v1/devices/{id}/config")
  Object config(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
    return views.one(service.config(a, id), Responses.DeviceConfig.class);
  }

  @PatchMapping("/api/v1/devices/{id}/config")
  Object patch(@AuthenticationPrincipal Actor a, @PathVariable UUID id, @RequestBody JsonNode n) {
    return views.one(
        service.patch(a, id, DeviceDtos.ConfigPatch.parse(n)), Responses.DeviceConfig.class);
  }

  @PostMapping("/api/v1/devices/{id}/config/ack")
  Object ack(@AuthenticationPrincipal Actor a, @PathVariable UUID id, @RequestBody JsonNode n) {
    Json.fields(n, "binding_id,reported_applied_version", "binding_id,reported_applied_version");
    return views.one(
        service.ack(
            a,
            id,
            Json.uuid(n, "binding_id", false),
            Json.number(n, "reported_applied_version", 1, 9007199254740991L)),
        Responses.DeviceConfig.class);
  }

  @PostMapping("/api/v1/devices/{id}/reports")
  Object report(@AuthenticationPrincipal Actor a, @PathVariable UUID id, @RequestBody JsonNode n) {
    return service.report(a, id, DeviceDtos.Report.parse(n));
  }

  @PostMapping("/api/v1/admin/devices")
  ResponseEntity<?> create(@RequestBody JsonNode n) {
    return ResponseEntity.status(201)
        .body(views.one(service.create(DeviceDtos.Create.parse(n)), Responses.AdminDevice.class));
  }

  @GetMapping("/api/v1/admin/devices")
  Object admin(@AuthenticationPrincipal Actor a, @RequestParam Map<String, String> q) {
    return views.page(service.list(a, q, false, true), Responses.AdminDevice.class);
  }

  @PatchMapping("/api/v1/admin/devices/{id}/status")
  Object status(@PathVariable UUID id, @RequestBody JsonNode n) {
    Json.fields(n, "status", "status");
    return views.one(
        service.status(id, Json.choice(n, "status", "ACTIVE,DISABLED,RETIRED")),
        Responses.AdminDevice.class);
  }
}
