package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

public final class Errors {
  private Errors() {}

  public static UUID id(HttpServletRequest r) {
    Object v = r.getAttribute("request_id");
    if (v instanceof UUID u) return u;
    UUID u = UUID.randomUUID();
    r.setAttribute("request_id", u);
    return u;
  }

  public static ApiError body(HttpServletRequest r, int s, String c) {
    return new ApiError(
        s,
        c,
        s == 409
            ? "Dữ liệu đã thay đổi. Vui lòng tải lại."
            : s == 400
                ? "Invalid request"
                : s >= 500 ? "Request could not be completed" : "Access denied",
        id(r));
  }

  public static void write(
      ObjectMapper m, HttpServletRequest r, HttpServletResponse p, int s, String c)
      throws IOException {
    p.setStatus(s);
    p.setContentType("application/json");
    p.setHeader("X-Request-ID", id(r).toString());
    m.writeValue(p.getOutputStream(), body(r, s, c));
  }
}
