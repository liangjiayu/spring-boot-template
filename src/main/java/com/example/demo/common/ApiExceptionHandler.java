package com.example.demo.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private ResponseEntity<ApiErrorResponse> error(int status, String message) {
    return error(status, message, null);
  }

  private ResponseEntity<ApiErrorResponse> error(
      int status, String message, Map<String, String> fieldErrors) {
    return ResponseEntity.status(status).body(new ApiErrorResponse(status, message, fieldErrors));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> business(ApiException ex) {
    return error(400, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
    return error(400, "请求参数校验失败", fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiErrorResponse> malformed() {
    return error(400, "请求格式或参数类型错误");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> dataIntegrity() {
    return error(400, "数据不符合约束");
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> status(ResponseStatusException ex) {
    return error(ex.getStatusCode().value(), ex.getReason());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> notFound() {
    return error(404, "请求路径不存在");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> unexpected(Exception ex, HttpServletRequest request) {
    log.error("Request failed: {}", request.getRequestURI(), ex);
    return error(500, "服务器内部错误");
  }
}
