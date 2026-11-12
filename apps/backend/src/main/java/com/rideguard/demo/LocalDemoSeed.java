package com.rideguard.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.auth.*;
import com.rideguard.devices.*;
import com.rideguard.feedback.*;
import com.rideguard.shared.Json;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit local CLI only. Never runs in the ordinary local/production HTTP server. */
@Component
@Profile("local & seed-demo")
public class LocalDemoSeed implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final AuthRepository auth;
  private final PasswordEncoder passwords;
  private final DevicesService devices;
  private final FeedbackService feedback;
  private final TransactionTemplate tx;
  private final Environment env;
  private final Clock clock;
  private final ObjectMapper mapper;
  private final ConfigurableApplicationContext context;

  public LocalDemoSeed(JdbcTemplate jdbc, AuthRepository auth, PasswordEncoder passwords,
      DevicesService devices, FeedbackService feedback, TransactionTemplate tx, Environment env,
      Clock clock, ObjectMapper mapper, ConfigurableApplicationContext context) {
    this.jdbc = jdbc; this.auth = auth; this.passwords = passwords; this.devices = devices;
    this.feedback = feedback; this.tx = tx; this.env = env; this.clock = clock;
    this.mapper = mapper; this.context = context;
  }

  static UUID key(String name) {
    return UUID.nameUUIDFromBytes(("RideGuard/local-demo/v1/" + name).getBytes(StandardCharsets.UTF_8));
  }

  static boolean allowedTarget(String url, String user) {
    return "rideguard".equals(user) && Set.of(
        "jdbc:postgresql://127.0.0.1:5433/rideguard",
        "jdbc:postgresql://localhost:5433/rideguard").contains(url == null ? "" : url);
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      if (!allowedTarget(env.getProperty("spring.datasource.url"), env.getProperty("spring.datasource.username")))
        throw new IllegalStateException("Seed requires local rideguard DB/user on port 5433, without URL parameters.");
      List<String> results = tx.execute(status -> seed());
      Objects.requireNonNull(results).forEach(System.out::println);
      System.out.println("Demo seed committed. No ACK or telemetry generated.");
    } finally { context.close(); }
  }

  private List<String> seed() {
    // Serialize seed commands; all writes, including service calls and markers, commit together.
    jdbc.execute("SELECT pg_advisory_xact_lock(74281932013)");
    var results = new ArrayList<String>();
    String email = env.getProperty("DEMO_ADMIN_EMAIL", "").strip().toLowerCase(Locale.ROOT);
    var admins = jdbc.queryForList("SELECT id FROM rideguard.users WHERE role='ADMIN' AND status='ACTIVE'"
        + (email.isEmpty() ? "" : " AND email=?"), email.isEmpty() ? new Object[] {} : new Object[] {email});
    if (admins.size() != 1)
      throw new IllegalStateException("Select exactly one existing ACTIVE ADMIN with DEMO_ADMIN_EMAIL; provision via CLI if none exists.");
    Actor admin = new Actor((UUID) admins.getFirst().get("id"), null, "ADMIN");
    results.add("ADMIN: reused existing account (no password or role changes)");
    Actor[] users = new Actor[2];
    for (int i = 0; i < 2; i++) {
      String demoEmail = "demo.user" + (i + 1) + "@rideguard.example";
      UUID id = key("user-" + (i + 1));
      var byEmail = auth.email(demoEmail);
      var byId = auth.user(id, true);
      if (byEmail == null && byId == null) {
        String password = env.getProperty("DEMO_USER" + (i + 1) + "_PASSWORD");
        if (password == null || password.codePointCount(0, password.length()) < 12
            || password.codePointCount(0, password.length()) > 128)
          throw new IllegalStateException("Supply DEMO_USER" + (i + 1) + "_PASSWORD (12-128 Unicode characters) through secret environment/prompt.");
        var input = AuthDtos.Register.parse(mapper.valueToTree(Json.obj("email", demoEmail,
            "full_name", "RideGuard Demo User " + (i + 1), "password", password)));
        // Same repository and PasswordEncoder as the existing provisioning CLI; no plaintext/hash fixture.
        auth.register(id, input.email(), input.fullName(), passwords.encode(input.password()), "USER", clock.instant());
        users[i] = new Actor(id, null, "USER");
        results.add("CREATED user " + demoEmail);
      } else if (byId != null && byEmail != null && id.equals(byEmail.get("id"))
          && "USER".equals(byId.get("role")) && "ACTIVE".equals(byId.get("status"))) {
        users[i] = new Actor(id, null, "USER");
        results.add("SKIPPED existing demo user " + demoEmail + " (password/profile unchanged)");
      } else results.add("SKIPPED conflicting/modified/locked user " + demoEmail + " and dependent demo actions");
    }
    for (int i = 1; i <= 5; i++) {
      String serial = String.format(Locale.ROOT, "RG-DEMO-%03d", i);
      if (jdbc.queryForObject("SELECT count(*) FROM rideguard.devices WHERE serial_number=?", Integer.class, serial) != 0) {
        results.add("SKIPPED existing device " + serial + " (status, owner, config unchanged)");
        continue;
      }
      var created = mapper.valueToTree(devices.create(DeviceDtos.Create.parse(mapper.valueToTree(
          Json.obj("serial_number", serial, "model", "RideGuard Demo", "hardware_revision", "DEMO-V1")))));
      UUID id = UUID.fromString(created.get("id").asText());
      String deviceStatus = i == 4 ? "DISABLED" : i == 5 ? "RETIRED" : "ACTIVE";
      if (!deviceStatus.equals("ACTIVE")) devices.status(id, deviceStatus);
      results.add("CREATED device " + serial + " " + deviceStatus);
      if (i <= 2 && users[i - 1] != null) {
        devices.bind(users[i - 1], key("bind-" + i), new DeviceDtos.Bind(serial, "Gương demo " + i));
        results.add("CREATED binding/config/issued version 1 for " + serial + "; applied=null");
      }
    }
    String[] states = {"OPEN", "IN_PROGRESS", "RESOLVED"};
    String[] contents = {"[DEMO] Đề xuất hướng dẫn sử dụng cấu hình gương.",
        "[DEMO] Cần hỗ trợ tìm hiểu trạng thái liên kết thiết bị.",
        "[DEMO] Đã nhận hướng dẫn sử dụng trang quản trị."};
    for (int i = 0; i < 3; i++) {
      Actor user = users[i % 2];
      if (user == null) { results.add("SKIPPED feedback " + (i + 1) + " (demo user unavailable)"); continue; }
      UUID idem = key("feedback-" + (i + 1));
      if (jdbc.queryForObject("SELECT count(*) FROM rideguard.idempotency_records WHERE user_id=? AND route='/api/v1/feedbacks' AND idempotency_key=?",
          Integer.class, user.userId(), idem) != 0) {
        results.add("SKIPPED existing feedback " + (i + 1) + " (content/status/reply unchanged)"); continue;
      }
      var result = feedback.create(user, idem, new FeedbackDtos.Create(null, "SUGGESTION", contents[i]));
      UUID id = (UUID) result.body().get("id");
      if (i > 0) feedback.patch(admin, id, new FeedbackDtos.Patch(states[i],
          i == 1 ? "[DEMO] Đã tiếp nhận yêu cầu, đang hướng dẫn người dùng." : "[DEMO] Đã cung cấp hướng dẫn và hoàn tất hỗ trợ."));
      results.add("CREATED feedback " + (i + 1) + " " + states[i]);
    }
    return results;
  }
}
