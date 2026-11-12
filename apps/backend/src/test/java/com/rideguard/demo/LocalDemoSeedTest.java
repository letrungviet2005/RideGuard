package com.rideguard.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.auth.AuthRepository;
import com.rideguard.devices.DevicesService;
import com.rideguard.feedback.FeedbackService;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class LocalDemoSeedTest {
  @Test void restrictsSeedToExplicitLocalDevelopmentTarget() {
    assertTrue(LocalDemoSeed.allowedTarget("jdbc:postgresql://127.0.0.1:5433/rideguard", "rideguard"));
    assertTrue(LocalDemoSeed.allowedTarget("jdbc:postgresql://localhost:5433/rideguard", "rideguard"));
    for (String url : new String[] {"jdbc:postgresql://remote:5433/rideguard",
        "jdbc:postgresql://127.0.0.1:5432/rideguard", "jdbc:postgresql://127.0.0.1:5433/other",
        "jdbc:postgresql://127.0.0.1:5433/rideguard?currentSchema=public", ""})
      assertFalse(LocalDemoSeed.allowedTarget(url, "rideguard"));
    assertFalse(LocalDemoSeed.allowedTarget("jdbc:postgresql://127.0.0.1:5433/rideguard", "postgres"));
  }
  @Test void markersAreStableAndDistinctAcrossResources() {
    assertEquals(LocalDemoSeed.key("feedback-1"), LocalDemoSeed.key("feedback-1"));
    assertNotEquals(LocalDemoSeed.key("feedback-1"), LocalDemoSeed.key("feedback-2"));
    assertNotEquals(LocalDemoSeed.key("user-1"), LocalDemoSeed.key("bind-1"));
  }

  @Test void rerunSkipsExistingResourcesWithoutResettingEditsOrPasswords() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    AuthRepository auth = mock(AuthRepository.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    DevicesService devices = mock(DevicesService.class);
    FeedbackService feedback = mock(FeedbackService.class);
    TransactionTemplate tx = mock(TransactionTemplate.class);
    Environment env = mock(Environment.class);
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    when(env.getProperty("spring.datasource.url")).thenReturn("jdbc:postgresql://127.0.0.1:5433/rideguard");
    when(env.getProperty("spring.datasource.username")).thenReturn("rideguard");
    when(env.getProperty("DEMO_ADMIN_EMAIL", "")).thenReturn("");
    when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("id", UUID.randomUUID())));
    when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
    for (int i = 1; i <= 2; i++) {
      UUID id = LocalDemoSeed.key("user-" + i);
      Map<String,Object> row = Map.of("id", id, "role", "USER", "status", "ACTIVE", "full_name", "Edited name");
      when(auth.email("demo.user" + i + "@rideguard.example")).thenReturn(row);
      when(auth.user(id, true)).thenReturn(row);
    }
    when(tx.execute(any())).thenAnswer(invocation -> {
      TransactionCallback<?> callback = invocation.getArgument(0);
      return callback.doInTransaction(mock(TransactionStatus.class));
    });
    new LocalDemoSeed(jdbc, auth, passwords, devices, feedback, tx, env,
        Clock.systemUTC(), new ObjectMapper(), context).run(null);
    verifyNoInteractions(passwords, devices, feedback);
    verify(auth, never()).register(any(), anyString(), anyString(), anyString(), anyString(), any());
    verify(context).close();
  }
}
