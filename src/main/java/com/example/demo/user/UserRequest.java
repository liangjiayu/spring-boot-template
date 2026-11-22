package com.example.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "用户新增或完整更新请求")
public record UserRequest(
    @Schema(description = "用户名，唯一", example = "alice") @NotBlank @Size(max = 50) String username,
    @Schema(description = "邮箱，空白值清空邮箱", example = "alice@example.com") @Email @Size(max = 255)
        String email,
    @Schema(description = "年龄", example = "20") @PositiveOrZero Integer age) {
  public UserRequest {
    if (email != null && email.isBlank()) email = null;
  }
}
