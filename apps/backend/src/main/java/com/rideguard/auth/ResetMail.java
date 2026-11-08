package com.rideguard.auth;

public interface ResetMail {
  void send(String email, String token);
}
