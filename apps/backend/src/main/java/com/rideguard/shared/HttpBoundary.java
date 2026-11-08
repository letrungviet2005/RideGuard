package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpBoundary extends OncePerRequestFilter {
  private final ObjectMapper mapper;
  private final Set<String> origins;
  private final int rate;
  private final Clock clock;
  private final Map<String, long[]> windows = new HashMap<>();

  public HttpBoundary(
      ObjectMapper m,
      @Value("${rideguard.allowed-origins:}") String origins,
      @Value("${rideguard.rate-limit-per-minute:120}") int rate,
      Clock c) {
    mapper = m;
    if (rate < 1) throw new IllegalStateException("Rate limit must be positive");
    this.origins = new HashSet<>(Arrays.asList(origins.split(",")));
    this.rate = rate;
    clock = c;
  }

  private synchronized boolean allow(String key) {
    long minute = clock.millis() / 60000;
    windows.entrySet().removeIf(e -> e.getValue()[0] != minute);
    if (windows.size() >= 10000 && !windows.containsKey(key)) return false;
    long[] w = windows.computeIfAbsent(key, k -> new long[] {minute, 0});
    return ++w[1] <= rate;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse p, FilterChain chain)
      throws ServletException, IOException {
    p.setHeader("X-Request-ID", Errors.id(q).toString());
    try {
      if (q.getRequestURI().startsWith("/api/")) {
        p.setHeader("Cache-Control", "no-store");
        if (!q.getMethod().equals("OPTIONS") && !allow(q.getRemoteAddr())) {
          p.setHeader("Retry-After", "60");
          throw new ApiException(429, "RATE_LIMITED");
        }
        String origin = q.getHeader("Origin");
        if (origin != null && !origins.contains(origin)) throw new ApiException(403, "FORBIDDEN");
        if ("OPTIONS".equals(q.getMethod()) && origin != null) {
          if (!Set.of("GET", "POST", "PATCH", "DELETE", "OPTIONS")
              .contains(Objects.toString(q.getHeader("Access-Control-Request-Method"), "")))
            throw new ApiException(403, "FORBIDDEN");
          String requestedHeaders = q.getHeader("Access-Control-Request-Headers");
          if (requestedHeaders != null) {
            for (String header : requestedHeaders.split(",")) {
              if (!Set.of(
                      "authorization",
                      "content-type",
                      "x-client-type",
                      "x-csrf-protection",
                      "idempotency-key")
                  .contains(header.strip().toLowerCase(Locale.ROOT)))
                throw new ApiException(403, "FORBIDDEN");
            }
          }
        }
        boolean web =
            "admin_web".equals(q.getHeader("X-Client-Type"))
                && Set.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout")
                    .contains(q.getRequestURI());
        if (web
            && (origin == null
                || !origins.contains(origin)
                || !"1".equals(q.getHeader("X-CSRF-Protection"))))
          throw new ApiException(403, "FORBIDDEN");
        if (Set.of("POST", "PATCH", "PUT", "DELETE").contains(q.getMethod())) {
          String encoding = q.getHeader("Content-Encoding");
          if (encoding != null && !encoding.equalsIgnoreCase("identity"))
            throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE");
          byte[] body = q.getInputStream().readNBytes(32769);
          if (body.length > 32768) throw new ApiException(413, "PAYLOAD_TOO_LARGE");
          if (body.length > 0 && q.getContentType() != null) {
            var media = org.springframework.http.MediaType.parseMediaType(q.getContentType());
            if (media.getCharset() != null
                && !media.getCharset().equals(java.nio.charset.StandardCharsets.UTF_8))
              throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE");
          }
          if (body.length > 0
              && (q.getContentType() == null
                  || !org.springframework.http.MediaType.parseMediaType(q.getContentType())
                      .isCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON)))
            throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE");
          q = new Buffered(q, body);
        }
      }
      chain.doFilter(q, p);
    } catch (ApiException e) {
      Errors.write(mapper, q, p, e.status, e.code);
    } catch (org.springframework.http.InvalidMediaTypeException e) {
      Errors.write(mapper, q, p, 415, "UNSUPPORTED_MEDIA_TYPE");
    }
  }

  static class Buffered extends HttpServletRequestWrapper {
    private final byte[] data;

    Buffered(HttpServletRequest q, byte[] d) {
      super(q);
      data = d;
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream in = new ByteArrayInputStream(data);
      return new ServletInputStream() {
        public int read() {
          return in.read();
        }

        public boolean isFinished() {
          return in.available() == 0;
        }

        public boolean isReady() {
          return true;
        }

        public void setReadListener(ReadListener l) {
          throw new UnsupportedOperationException("Synchronous JSON requests only");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(
          new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
    }
  }
}
