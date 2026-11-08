package com.rideguard.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.rideguard.shared.*;
import java.util.UUID;

public final class FeedbackDtos {
  private FeedbackDtos() {}

  public record Create(UUID device, String category, String content) {
    public static Create parse(JsonNode n) {
      Json.fields(n, "device_id,category,content", "device_id,category,content");
      return new Create(
          Json.uuid(n, "device_id", true),
          Json.choice(n, "category", "BUG,SUGGESTION,OTHER"),
          Json.str(n, "content", 1, 5000, false, true));
    }
  }

  public record Patch(String status, String response) {
    public static Patch parse(JsonNode n) {
      Json.fields(n, "status,admin_response", "");
      if (n.isEmpty()) throw ApiException.bad();
      return new Patch(
          n.has("status") ? Json.choice(n, "status", "OPEN,IN_PROGRESS,RESOLVED") : null,
          n.has("admin_response") ? Json.str(n, "admin_response", 1, 5000, false, true) : null);
    }
  }
}
