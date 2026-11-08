package com.rideguard.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rideguard.shared.*;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class Tokens {
  private final NimbusJwtEncoder encoder;
  private final NimbusJwtDecoder decoder;
  private final Clock clock;

  public Tokens(@Value("${rideguard.jwt-key}") String key, Clock clock) {
    this.clock = clock;
    byte[] k = Crypto.key(key);
    encoder = new NimbusJwtEncoder(new ImmutableSecret<>(k));
    decoder =
        NimbusJwtDecoder.withSecretKey(new SecretKeySpec(k, "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    var timestamp = new JwtTimestampValidator(Duration.ZERO);
    timestamp.setClock(clock);
    decoder.setJwtValidator(
        new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
            timestamp, new JwtIssuerValidator("rideguard")));
  }

  public String issue(UUID user, UUID family) {
    Instant now = clock.instant();
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                    .issuer("rideguard")
                    .subject(user.toString())
                    .audience(List.of("rideguard-api"))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(900))
                    .id(UUID.randomUUID().toString())
                    .claim("sid", family.toString())
                    .build()))
        .getTokenValue();
  }

  public Jwt verify(String token) {
    try {
      Jwt j = decoder.decode(token);
      Instant now = clock.instant();
      if (!"rideguard".equals(j.getClaimAsString("iss"))
          || !j.getAudience().contains("rideguard-api")
          || j.getExpiresAt() == null
          || !j.getExpiresAt().isAfter(now)
          || j.getIssuedAt() == null
          || j.getIssuedAt().isAfter(now.plusSeconds(30))
          || j.getExpiresAt().isAfter(j.getIssuedAt().plusSeconds(900)))
        throw ApiException.unauthorized();
      return j;
    } catch (Exception e) {
      throw ApiException.unauthorized();
    }
  }
}
