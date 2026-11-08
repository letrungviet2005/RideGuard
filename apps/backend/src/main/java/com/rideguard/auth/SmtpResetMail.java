package com.rideguard.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpResetMail implements ResetMail {
  private final JavaMailSender sender;
  private final String from;

  public SmtpResetMail(JavaMailSender sender, @Value("${rideguard.mail-from}") String from) {
    this.sender = sender;
    this.from = from;
  }

  public void send(String email, String token) {
    SimpleMailMessage m = new SimpleMailMessage();
    m.setFrom(from);
    m.setTo(email);
    m.setSubject("RideGuard password reset");
    m.setText("Use this one-time reset code within 15 minutes: " + token);
    sender.send(m);
  }
}
