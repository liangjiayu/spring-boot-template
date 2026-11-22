package com.example.app.common;

public final class ApiException extends RuntimeException {
  public ApiException(String message) {
    super(message);
  }
}
