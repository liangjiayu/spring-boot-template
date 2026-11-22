package com.example.demo.common;

public final class ApiException extends RuntimeException {
  public ApiException(String message) {
    super(message);
  }
}
