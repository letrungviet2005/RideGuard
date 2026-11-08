package com.rideguard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideguard.shared.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerFilter extends OncePerRequestFilter {
  private final AuthService auth;
  private final ObjectMapper mapper;

  public BearerFilter(AuthService a, ObjectMapper m) {
    auth = a;
    mapper = m;
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse p, FilterChain chain)
      throws ServletException, IOException {
    String h = q.getHeader("Authorization");
    if (h != null) {
      try {
        if (!h.startsWith("Bearer ")) throw ApiException.unauthorized();
        Actor a = auth.authenticate(h.substring(7));
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    a, null, List.of(new SimpleGrantedAuthority("ROLE_" + a.role()))));
      } catch (ApiException e) {
        Errors.write(mapper, q, p, e.status, e.code);
        return;
      } catch (org.springframework.dao.DataAccessException e) {
        Errors.write(mapper, q, p, 503, "TEMPORARILY_UNAVAILABLE");
        return;
      }
    }
    chain.doFilter(q, p);
  }
}
