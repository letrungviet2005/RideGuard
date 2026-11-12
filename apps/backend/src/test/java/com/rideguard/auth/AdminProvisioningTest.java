package com.rideguard.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.ApiException;
import com.rideguard.shared.Json;
import org.junit.jupiter.api.Test;

class AdminProvisioningTest {
  @Test
  void consolePasswordLimitsMatchApiIncludingUnicodeAndSpaces() {
    var mapper = new ObjectMapper();
    for (String value : new String[] {
        "", "a".repeat(11), "a".repeat(12), "a".repeat(128), "a".repeat(129),
        "\uD83D\uDE80".repeat(6), "\uD83D\uDE80".repeat(12), " a".repeat(6)
    }) {
      var body = mapper.createObjectNode().put("password", value);
      if (AdminProvisioning.validPasswordLength(value.toCharArray())) {
        assertEquals(value, Json.password(body, "password"));
      } else {
        assertThrows(ApiException.class, () -> Json.password(body, "password"));
      }
    }
    assertFalse(AdminProvisioning.validPasswordLength("\uD83D\uDE80".repeat(6).toCharArray()));
    assertTrue(AdminProvisioning.validPasswordLength("\uD83D\uDE80".repeat(12).toCharArray()));
  }
}
