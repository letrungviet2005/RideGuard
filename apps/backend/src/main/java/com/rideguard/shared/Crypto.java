package com.rideguard.shared;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Crypto {
  private static final SecureRandom RANDOM = new SecureRandom();

  private Crypto() {}

  public static String random() {
    byte[] b = new byte[32];
    RANDOM.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static byte[] key(String s) {
    try {
      byte[] b = Base64.getDecoder().decode(s);
      if (b.length < 32) throw new IllegalArgumentException();
      return b;
    } catch (Exception e) {
      throw new IllegalStateException("A base64 key of at least 32 bytes is required");
    }
  }

  public static String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static byte[] mac(byte[] key, String s) {
    try {
      Mac m = Mac.getInstance("HmacSHA256");
      m.init(new SecretKeySpec(key, "HmacSHA256"));
      return m.doFinal(s.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
