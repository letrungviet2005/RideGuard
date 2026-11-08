package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.auth.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ObjectMapper m,
      AuthService auth,
      @Value("${rideguard.allowed-origins:}") String origins,
      @Value("${rideguard.cookie-secure:true}") boolean secure,
      @Value("${rideguard.cookie-same-site:Lax}") String sameSite,
      Environment env)
      throws Exception {
    boolean local = Arrays.asList(env.getActiveProfiles()).contains("local");
    if (!secure && !local)
      throw new IllegalStateException("Insecure cookies require explicit local profile");
    if (!Set.of("Lax", "Strict", "None").contains(sameSite) || (!secure && sameSite.equals("None")))
      throw new IllegalStateException("Invalid cookie policy");
    List<String> allowed = Arrays.stream(origins.split(",")).filter(s -> !s.isBlank()).toList();
    if (allowed.stream()
        .anyMatch(
            s -> s.contains("*") || s.equals("null") || (!local && !s.startsWith("https://"))))
      throw new IllegalStateException("Exact HTTPS origins required outside local profile");
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(allowed);
    c.setAllowCredentials(true);
    c.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "X-Client-Type",
            "X-CSRF-Protection",
            "Idempotency-Key"));
    c.setExposedHeaders(List.of("X-Request-ID", "Retry-After"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", c);
    // This is a non-secret non-simple header, NOT a synchronizer secret.
    // HttpBoundary also requires an exact trusted Origin before this filter.
    // Mobile/body credentials and Bearer-only CRUD never authenticate from cookies.
    CsrfTokenRepository csrf =
        new CsrfTokenRepository() {
          public CsrfToken generateToken(HttpServletRequest r) {
            return new DefaultCsrfToken("X-CSRF-Protection", "_csrf", "1");
          }

          public CsrfToken loadToken(HttpServletRequest r) {
            return generateToken(r);
          }

          public void saveToken(CsrfToken t, HttpServletRequest q, HttpServletResponse p) {}
        };
    return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(ca -> ca.disable())
        .cors(co -> co.configurationSource(source))
        .csrf(
            cs ->
                cs.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher(
                        r ->
                            "admin_web".equals(r.getHeader("X-Client-Type"))
                                && "POST".equals(r.getMethod())
                                && Set.of(
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/logout")
                                    .contains(r.getRequestURI())))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness")
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, p, x) -> Errors.write(m, q, p, 401, "UNAUTHORIZED"))
                    .accessDeniedHandler((q, p, x) -> Errors.write(m, q, p, 403, "FORBIDDEN")))
        .addFilterBefore(new BearerFilter(auth, m), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
