package com.rideguard.shared;

import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {
  @Bean
  TransactionTemplate transactions(PlatformTransactionManager m) {
    return new TransactionTemplate(m);
  }
}
