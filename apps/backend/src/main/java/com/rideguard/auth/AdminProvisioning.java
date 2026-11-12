package com.rideguard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("provision-admin")
public class AdminProvisioning implements ApplicationRunner {
  private final AuthRepository repo;
  private final PasswordEncoder passwords;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final org.springframework.context.ConfigurableApplicationContext context;

  public AdminProvisioning(
      AuthRepository r,
      PasswordEncoder p,
      TransactionTemplate t,
      Clock c,
      org.springframework.context.ConfigurableApplicationContext context) {
    this.context = context;
    repo = r;
    passwords = p;
    tx = t;
    clock = c;
  }

  public void run(ApplicationArguments args) {
    var console = System.console();
    if (console == null)
      throw new IllegalStateException("Admin provisioning requires an interactive console");
    String email = console.readLine("Admin email: ");
    String name = console.readLine("Full name: ");
    char[] chars;
    while (true) {
      chars = console.readPassword("Admin password (12-128 Unicode characters, hidden): ");
      if (chars == null) {
        context.close();
        throw new IllegalStateException("Admin password input cancelled; no account created");
      }
      if (validPasswordLength(chars)) break;
      Arrays.fill(chars, '\0');
      console.printf("Password must contain 12-128 Unicode characters. Please try again.%n");
    }
    try {
      var n =
          new ObjectMapper()
              .valueToTree(
                  Json.obj("email", email, "full_name", name, "password", new String(chars)));
      var d = AuthDtos.Register.parse(n);
      tx.executeWithoutResult(
          s -> {
            if (repo.email(d.email()) != null)
              throw new IllegalStateException("Account already exists; no implicit promotion");
            repo.register(
                UUID.randomUUID(),
                d.email(),
                d.fullName(),
                passwords.encode(d.password()),
                "ADMIN",
                clock.instant());
          });
      console.printf("Admin created. Stop this process before starting HTTP service.%n");
    } finally {
      Arrays.fill(chars, '\0');
      context.close();
    }
  }
  static boolean validPasswordLength(char[] password) {
    int length = Character.codePointCount(password, 0, password.length);
    return length >= 12 && length <= 128;
  }
}
