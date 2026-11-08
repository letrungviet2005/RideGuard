package com.rideguard.shared;

public class ApiException extends RuntimeException {
  public final int status;
  public final String code;

  public ApiException(int status, String code) {
    super(code);
    this.status = status;
    this.code = code;
  }

  public static ApiException bad() {
    return new ApiException(400, "INVALID_REQUEST");
  }

  public static ApiException missing() {
    return new ApiException(404, "NOT_FOUND");
  }

  public static ApiException unauthorized() {
    return new ApiException(401, "UNAUTHORIZED");
  }
}
