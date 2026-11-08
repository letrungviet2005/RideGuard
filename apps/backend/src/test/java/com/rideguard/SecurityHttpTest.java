package com.rideguard;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.rideguard.auth.*;
import com.rideguard.shared.*;
import com.rideguard.users.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {AuthController.class, UsersController.class},
    properties = {
      "rideguard.allowed-origins=https://admin.example.invalid",
      "rideguard.rate-limit-per-minute=10000"
    })
@Import({
  SecurityConfig.class,
  HttpBoundary.class,
  AppConfig.class,
  ApiExceptionHandler.class,
  ResponseViews.class
})
class SecurityHttpTest {
  @Test
  void invalidCorsPreflightHasErrorEnvelope() throws Exception {
    mvc.perform(
            options("/api/v1/users/me")
                .header("Origin", "https://admin.example.invalid")
                .header("Access-Control-Request-Method", "PUT"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.request_id").exists());
  }

  @Autowired MockMvc mvc;
  @MockitoBean AuthService auth;
  @MockitoBean UsersService users;

  @Test
  void publicRegisterAndUnknownFields() throws Exception {
    when(auth.register(any())).thenReturn(Json.obj("id", UUID.randomUUID()));
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(
                    "{\"email\":\"a@b.invalid\",\"full_name\":\"A\",\"password\":\"123456789012\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Request-ID"));
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.request_id").exists());
  }

  @Test
  void unauthorizedAndRoleEnforced() throws Exception {
    mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    when(auth.authenticate("user-token"))
        .thenReturn(new Actor(UUID.randomUUID(), UUID.randomUUID(), "USER"));
    mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer user-token"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/missing").header("Authorization", "Bearer user-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  void webRequiresOriginAndCsrf() throws Exception {
    for (String origin : List.of("https://evil.invalid", "null")) {
      mvc.perform(
              post("/api/v1/auth/login")
                  .header("X-Client-Type", "admin_web")
                  .header("Origin", origin)
                  .header("X-CSRF-Protection", "1")
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isForbidden());
    }
    mvc.perform(
            post("/api/v1/auth/login")
                .header("X-Client-Type", "admin_web")
                .header("Origin", "https://admin.example.invalid")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void cookieCannotAuthenticateMobile() throws Exception {
    mvc.perform(
            post("/api/v1/auth/refresh")
                .header("X-Client-Type", "mobile")
                .cookie(new jakarta.servlet.http.Cookie("rg_refresh", "x".repeat(43)))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
    verify(auth, never()).refresh(any(), any());
  }

  @Test
  void webTransportHidesRefresh() throws Exception {
    var body =
        Json.obj(
            "access_token",
            "synthetic-access",
            "token_type",
            "Bearer",
            "expires_in",
            900,
            "user",
            Map.of());
    when(auth.login(any(), eq("admin_web")))
        .thenReturn(
            new AuthService.Grant(
                body, "synthetic-refresh-value", java.time.Instant.now().plusSeconds(100)));
    mvc.perform(
            post("/api/v1/auth/login")
                .header("X-Client-Type", "admin_web")
                .header("Origin", "https://admin.example.invalid")
                .header("X-CSRF-Protection", "1")
                .contentType("application/json")
                .content("{\"email\":\"a@b.invalid\",\"password\":\"123456789012\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refresh_token").doesNotExist())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("HttpOnly"))))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void preflightAndWrongMedia() throws Exception {
    mvc.perform(
            options("/api/v1/users/me")
                .header("Origin", "https://admin.example.invalid")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://admin.example.invalid"));
    mvc.perform(post("/api/v1/auth/register").contentType("text/plain").content("{}"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void actualBodyLimitWithoutLength() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content("x".repeat(32769))
                .with(
                    r -> {
                      r.removeHeader("Content-Length");
                      return r;
                    }))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
  }

  @Test
  void methodAndValidationNot500() throws Exception {
    when(auth.authenticate("token"))
        .thenReturn(new Actor(UUID.randomUUID(), UUID.randomUUID(), "USER"));
    mvc.perform(
            put("/api/v1/users/me")
                .header("Authorization", "Bearer token")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{"))
        .andExpect(status().isBadRequest());
  }
}
