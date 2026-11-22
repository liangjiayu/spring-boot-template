package com.example.app.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorSchema", description = "统一错误响应")
public record ApiErrorResponse(
    @Schema(description = "HTTP 状态码", example = "400", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
    @Schema(description = "错误说明", example = "请求参数校验失败", requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {}
