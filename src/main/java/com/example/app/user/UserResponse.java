package com.example.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(
    description = "用户记录",
    requiredProperties = {"id", "username", "createdAt", "updatedAt"})
public record UserResponse(
    @Schema(description = "用户 ID", example = "1") long id,
    @Schema(description = "用户名", maxLength = 50, example = "alice") String username,
    @Schema(description = "邮箱", maxLength = 255, example = "alice@example.com") String email,
    @Schema(description = "年龄", minimum = "0", example = "20") Integer age,
    @Schema(description = "创建时间", example = "2026-11-20T10:00:00+08:00") OffsetDateTime createdAt,
    @Schema(description = "更新时间") OffsetDateTime updatedAt) {}
