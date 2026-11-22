package com.example.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "用户新增或完整更新请求")
public record UserRequest(
    @Schema(description = "用户名，唯一", example = "alice")
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过 {max} 个字符")
        String username,
    @Schema(description = "邮箱，空白值清空邮箱", example = "alice@example.com")
        @Email(message = "邮箱格式不正确")
        @Size(max = 255, message = "邮箱长度不能超过 {max} 个字符")
        String email,
    @Schema(description = "年龄", example = "20") @PositiveOrZero(message = "年龄不能为负数") Integer age) {
  public UserRequest {
    if (email != null && email.isBlank()) email = null;
  }
}
