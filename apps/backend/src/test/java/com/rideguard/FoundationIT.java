package com.rideguard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
class FoundationIT extends PostgresIT {
  @Autowired MockMvc mvc;

  @Test
  void probesAreMinimalAndDatabaseIsReady() throws Exception {
    for (String path :
        new String[] {
          "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
        }) {
      mvc.perform(get(path))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").doesNotExist())
          .andExpect(jsonPath("$.details").doesNotExist());
    }
  }

  @Test
  void protectedApisRequireBearer() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    mvc.perform(post("/api/v1/devices/bind"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }
}
