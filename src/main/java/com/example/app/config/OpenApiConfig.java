package com.example.app.config;

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
  OpenAPI openApi() {
    var components = new Components();
    ModelConverters.getInstance().read(ApiErrorResponse.class).forEach(components::addSchemas);

    var info =
        new Info().title("spring-boot-template").version("v1").description("基于 Spring Boot 的项目模板");
    return new OpenAPI().info(info).components(components);
  }
}
