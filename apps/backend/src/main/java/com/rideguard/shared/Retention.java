package com.rideguard.shared;

import java.sql.Timestamp;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
public class Retention {
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final int days;

  public Retention(
      JdbcTemplate j, Clock c, @Value("${rideguard.reports-retention-days:30}") int days) {
    if (days < 2) throw new IllegalStateException("Report retention must be at least two days");
    jdbc = j;
    clock = c;
    this.days = days;
  }

  @Scheduled(
      fixedDelayString = "${rideguard.cleanup-delay-ms:3600000}",
      initialDelayString = "${rideguard.cleanup-delay-ms:3600000}")
  @Transactional
  public void clean() {
    Instant now = clock.instant();
    jdbc.update(
        "DELETE FROM rideguard.device_reports WHERE received_at<?",
        Timestamp.from(now.minus(Duration.ofDays(days))));
    jdbc.update(
        "UPDATE rideguard.idempotency_records SET response_body=NULL,state='EXPIRED' WHERE"
            + " response_expires_at<=? AND state='COMPLETED'",
        Timestamp.from(now));
  }
}
