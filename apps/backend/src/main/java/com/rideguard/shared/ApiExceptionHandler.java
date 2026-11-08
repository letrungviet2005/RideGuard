package com.rideguard.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handle(Exception e, HttpServletRequest r) {
    int status = 500;
    String code = "INTERNAL_ERROR";
    if (e instanceof ApiException a) {
      status = a.status;
      code = a.code;
    } else if (e instanceof ErrorResponse a) {
      status = a.getStatusCode().value();
      code =
          status == 404
              ? "NOT_FOUND"
              : status == 405
                  ? "METHOD_NOT_ALLOWED"
                  : status == 415 ? "UNSUPPORTED_MEDIA_TYPE" : "INVALID_REQUEST";
    } else if (e instanceof org.springframework.http.converter.HttpMessageNotReadableException
        || e instanceof ConstraintViolationException
        || e
            instanceof
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
      status = 400;
      code = "INVALID_REQUEST";
    } else if (e instanceof org.springframework.security.access.AccessDeniedException) {
      status = 403;
      code = "FORBIDDEN";
    } else if (e instanceof DuplicateKeyException) {
      status = 409;
      code = "CONFLICT";
    } else if (e instanceof TransientDataAccessException
        || e instanceof org.springframework.jdbc.CannotGetJdbcConnectionException) {
      status = 503;
      code = "TEMPORARILY_UNAVAILABLE";
    }
    return ResponseEntity.status(status).body(Errors.body(r, status, code));
  }
}
