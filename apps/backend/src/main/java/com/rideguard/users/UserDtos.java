package com.rideguard.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.shared.Json;

public final class UserDtos {
  private UserDtos() {}

  public record Profile(String fullName) {
    public static Profile parse(JsonNode input) {
      Json.fields(input, "full_name", "full_name");
      return new Profile(Json.str(input, "full_name", 1, 100, false, true));
    }
  }

  public record ChangePassword(String currentPassword, String newPassword) {
    public static ChangePassword parse(JsonNode input) {
      Json.fields(input, "current_password,new_password", "current_password,new_password");
      return new ChangePassword(
          Json.password(input, "current_password"), Json.password(input, "new_password"));
    }
  }
}
