package com.rideguard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.auth.*;
import com.rideguard.devices.*;
import com.rideguard.shared.*;
import com.rideguard.users.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

class ServiceRulesTest {
  final Instant now = Instant.parse("2026-11-07T00:00:00Z");
  final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
  final Actor actor = new Actor(UUID.randomUUID(), UUID.randomUUID(), "USER");
  final DevicesRepository devices = mock(DevicesRepository.class);
  final DevicesService service =
      new DevicesService(
          devices,
          mock(Idempotency.class),
          new Cursors(Base64.getEncoder().encodeToString(new byte[32]), new ObjectMapper()),
          clock);

  @Test
  void staleDeleteOnlyTouchesExpectedBinding() {
    UUID device = UUID.randomUUID(), old = UUID.randomUUID();
    when(devices.device(device)).thenReturn(Json.obj("id", device));
    when(devices.binding(old))
        .thenReturn(
            Json.obj(
                "id",
                old,
                "device_id",
                device,
                "user_id",
                actor.userId(),
                "unbound_at",
                now.minusSeconds(100)));
    service.unbind(actor, device, old);
    verify(devices).unbind(old, now);
    verify(devices, never()).active(any());
  }

  @Test
  void foreignBindingCannotBeClosed() {
    UUID device = UUID.randomUUID(), binding = UUID.randomUUID();
    when(devices.device(device)).thenReturn(Json.obj("id", device));
    when(devices.binding(binding))
        .thenReturn(Json.obj("device_id", device, "user_id", UUID.randomUUID()));
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.unbind(actor, device, binding)).status);
    verify(devices, never()).unbind(any(), any());
  }

  @Test
  void configCasPreventsWriteAndAckCannotInventVersion() {
    UUID device = UUID.randomUUID(), binding = UUID.randomUUID();
    when(devices.owned(actor.userId(), device)).thenReturn(Json.obj("id", binding));
    when(devices.device(device)).thenReturn(Json.obj("status", "ACTIVE"));
    when(devices.config(binding)).thenReturn(Json.obj("desired_version", 2));
    assertEquals(
        "VERSION_CONFLICT",
        assertThrows(
                ApiException.class,
                () ->
                    service.patch(
                        actor,
                        device,
                        new DeviceDtos.ConfigPatch(1, Map.of("display_brightness", 80))))
            .code);
    verify(devices, never()).config(any(), anyLong(), any(), any());
    assertEquals(
        "CONFIG_VERSION_NOT_ISSUED",
        assertThrows(ApiException.class, () -> service.ack(actor, device, binding, 3)).code);
    verify(devices, never()).ack(any(), anyLong(), any());
  }

  @Test
  void clockToleranceAndStaleReportsUseFixedClock() {
    UUID device = UUID.randomUUID(), binding = UUID.randomUUID();
    when(devices.owned(actor.userId(), device)).thenReturn(Json.obj("id", binding));
    when(devices.latest(binding)).thenReturn(Json.obj("observed_at", now));
    var stale =
        new DeviceDtos.Report(
            binding, now.minusSeconds(1), null, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", null);
    service.report(actor, device, stale);
    verify(devices).report(stale, now, false);
    var future =
        new DeviceDtos.Report(
            binding, now.plusSeconds(301), null, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", null);
    assertEquals(
        "OBSERVED_AT_OUT_OF_RANGE",
        assertThrows(ApiException.class, () -> service.report(actor, device, future)).code);
  }

  @Test
  void retiredDeviceCannotBecomeActive() {
    UUID device = UUID.randomUUID();
    when(devices.device(device)).thenReturn(Json.obj("status", "RETIRED"));
    assertEquals(
        "INVALID_STATUS_TRANSITION",
        assertThrows(ApiException.class, () -> service.status(device, "ACTIVE")).code);
    verify(devices, never()).status(any(), any(), any());
  }

  @Test
  void reuseCommitsRevocationBeforeReturningUnauthorized() {
    AuthRepository repo = mock(AuthRepository.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    AuthService auth =
        new AuthService(
            repo,
            passwords,
            mock(Tokens.class),
            clock,
            new TransactionTemplate(manager),
            mock(ResetMail.class),
            Runnable::run);
    UUID generation = UUID.randomUUID();
    var old =
        Json.obj(
            "id",
            generation,
            "user_id",
            actor.userId(),
            "family_id",
            actor.familyId(),
            "client_type",
            "mobile",
            "consumed_at",
            now.minusSeconds(1));
    when(repo.token(any())).thenReturn(old);
    when(repo.session(generation)).thenReturn(old);
    when(repo.user(actor.userId(), true)).thenReturn(Json.obj("status", "ACTIVE"));
    assertEquals(
        401,
        assertThrows(ApiException.class, () -> auth.refresh("synthetic-refresh-token", "mobile"))
            .status);
    verify(repo).revoke(actor.userId(), actor.familyId(), now);
    verify(manager).commit(any());
    verify(manager, never()).rollback(any());
  }

  @Test
  void lastAdminAndSelfLockProtected() {
    UsersRepository users = mock(UsersRepository.class);
    AuthRepository auth = mock(AuthRepository.class);
    UsersService service = new UsersService(users, auth, null, clock);
    UUID target = UUID.randomUUID();
    when(auth.user(target, true)).thenReturn(Json.obj("role", "ADMIN", "status", "ACTIVE"));
    when(users.activeAdmins()).thenReturn(1L);
    assertEquals(
        "LAST_ADMIN",
        assertThrows(ApiException.class, () -> service.status(actor, target, "LOCKED")).code);
    when(auth.user(actor.userId(), true)).thenReturn(Json.obj("role", "ADMIN", "status", "ACTIVE"));
    assertEquals(
        "ADMIN_SELF_LOCK",
        assertThrows(ApiException.class, () -> service.status(actor, actor.userId(), "LOCKED"))
            .code);
    verify(users, never()).status(any(), any(), any());
  }

  @Test
  void realArgon2PreservesPasswordAndUsesRandomSalt() {
    PasswordEncoder encoder = new AppConfig().passwords();
    String password = " 1234567890 ";
    String first = encoder.encode(password), second = encoder.encode(password);
    assertTrue(first.startsWith("$argon2id$"));
    assertNotEquals(first, second);
    assertTrue(encoder.matches(password, first));
    assertFalse(encoder.matches(password.strip(), first));
  }
}
