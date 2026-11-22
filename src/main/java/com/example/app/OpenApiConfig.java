package com.example.app;

import com.example.app.common.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI userOpenApi() {
    var components = new Components();
    ModelConverters.getInstance().read(ApiErrorResponse.class).forEach(components::addSchemas);
    return new OpenAPI()
        .components(components)
        .info(
            new Info()
                .title("CRUD API")
                .version("v1")
                .description(
                    "用户接口成功时返回 200。参数错误、用户不存在、用户名重复以及数据库约束冲突返回 400，未预期的服务端错误返回 500。错误体使用 ApiErrorSchema，包含 status 和 message，字段校验失败时附带 fieldErrors。"));
  }
}
