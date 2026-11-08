package com.rideguard.shared;

import com.fasterxml.jackson.databind.*;
import java.time.Clock;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {
  @Bean(name = "resetExecutor")
  public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor resetExecutor() {
    var e = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
    e.setCorePoolSize(1);
    e.setMaxPoolSize(2);
    e.setQueueCapacity(100);
    e.setThreadNamePrefix("reset-mail-");
    e.setWaitForTasksToCompleteOnShutdown(true);
    e.setAwaitTerminationSeconds(15);
    return e;
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public PasswordEncoder passwords() {
    return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
  }

  @Bean
  Jackson2ObjectMapperBuilderCustomizer jsonConfig() {
    return b ->
        b.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }
}
