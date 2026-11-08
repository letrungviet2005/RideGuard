package com.rideguard;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Runs only against an explicitly supplied, initially empty migration test database. */
@EnabledIfEnvironmentVariable(named = "TEST_MIGRATION_DB_URL", matches = ".+")
class MigrationIT {
  @Test
  void upgradesFoundationWithoutChangingV1OrMovingHistory() throws Exception {
    String url = System.getenv("TEST_MIGRATION_DB_URL");
    assertTrue(url.matches("jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test"));
    assertNotEquals(System.getenv("TEST_DB_URL"), url);
    String user = System.getenv("TEST_DB_USER"), password = System.getenv("TEST_DB_PASSWORD");
    try (var connection = DriverManager.getConnection(url, user, password);
        var statement = connection.createStatement();
        var result = statement.executeQuery("SELECT to_regclass('public.flyway_schema_history')")) {
      result.next();
      assertNull(
          result.getString(1),
          "Use a fresh dedicated migration database; never clean an existing one automatically");
    }
    Flyway foundation = Flyway.configure().dataSource(url, user, password).target("1").load();
    foundation.migrate();
    Integer checksum = foundation.info().current().getChecksum();
    Flyway full = Flyway.configure().dataSource(url, user, password).load();
    full.migrate();
    full.validate();
    assertEquals("2", full.info().current().getVersion().getVersion());
    assertEquals(checksum, full.info().applied()[0].getChecksum());
    try (var connection = DriverManager.getConnection(url, user, password);
        var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='rideguard'")) {
      result.next();
      assertEquals(10, result.getInt(1));
    }
  }
}
