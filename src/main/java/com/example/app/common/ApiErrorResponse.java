package com.example.app.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "ApiErrorSchema", description = "统一错误响应；fieldErrors 仅在字段校验失败时返回")
public record ApiErrorResponse(
    @Schema(description = "HTTP 状态码", example = "400", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
    @Schema(description = "错误说明", example = "请求参数校验失败", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,
    @Schema(description = "字段校验错误，其他错误不包含此字段", example = "{}")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, String> fieldErrors) {}
