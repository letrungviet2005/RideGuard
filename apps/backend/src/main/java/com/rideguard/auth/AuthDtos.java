package com.rideguard.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.shared.Json;

public final class AuthDtos {
  private AuthDtos() {}

  public record Register(String email, String fullName, String password) {
    public static Register parse(JsonNode n) {
      Json.fields(n, "email,full_name,password", "email,full_name,password");
      return new Register(
          Json.email(n),
          Json.str(n, "full_name", 1, 100, false, true),
          Json.password(n, "password"));
    }
  }

  public record Login(String email, String password, String clientName) {
    public static Login parse(JsonNode n) {
      Json.fields(n, "email,password,client_name", "email,password");
      return new Login(
          Json.email(n),
          Json.password(n, "password"),
          Json.str(n, "client_name", 1, 100, true, true));
    }
  }
}
