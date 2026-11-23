package com.example.app.config;

import static org.junit.jupiter.api.Assertions.*;

import com.example.app.support.HttpTestSupport;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class OpenApiTest extends HttpTestSupport {
  /**
   * 验证 OpenAPI 发布模板元信息和五项用户操作，各操作只声明 200 响应，且删除操作返回布尔值。
   */
  @Test
  void publishesTemplateInfoAndUserOperations() throws Exception {
    var spec = apiDocs();
    assertEquals("spring-boot-template", spec.at("/info/title").asString());
    assertEquals("v1", spec.at("/info/version").asString());
    assertEquals("基于 Spring Boot 的项目模板", spec.at("/info/description").asString());

    var paths = spec.path("paths");
    var collection = paths.path("/api/users");
    assertSuccessResponses(collection.path("get"));
    assertSuccessResponses(collection.path("post"));

    var member = paths.path("/api/users/{id}");
    assertSuccessResponses(member.path("get"));
    assertSuccessResponses(member.path("put"));
    assertSuccessResponses(member.path("delete"));
    assertEquals(
        "boolean", member.path("delete").at("/responses/200/content/*~1*/schema/type").asString());
  }

  /**
   * 验证 OpenAPI 模型准确声明用户请求的字段约束、用户响应的必填项和时间格式，以及通用错误响应结构。
   */
  @Test
  void documentsUserFieldsAndSharedErrorResponse() throws Exception {
    var schemas = apiDocs().at("/components/schemas");
    var request = schemas.path("UserRequest");
    assertEquals(Set.of("username"), requiredFields(request));
    assertEquals(50, request.at("/properties/username/maxLength").asInt());
    assertEquals(255, request.at("/properties/email/maxLength").asInt());
    assertEquals(0, request.at("/properties/age/minimum").asInt(-1));

    var response = schemas.path("UserResponse");
    assertEquals(Set.of("id", "username", "createdAt", "updatedAt"), requiredFields(response));
    assertEquals("string", response.at("/properties/email/type").asString());
    assertEquals("date-time", response.at("/properties/createdAt/format").asString());
    assertEquals("date-time", response.at("/properties/updatedAt/format").asString());

    var error = schemas.path("ApiErrorSchema");
    assertEquals(Set.of("status", "message"), requiredFields(error));
    assertEquals("integer", error.at("/properties/status/type").asString());
    assertEquals("string", error.at("/properties/message/type").asString());
    assertEquals(2, error.path("properties").size());
  }

  @Test
  void servesSwaggerUi() throws Exception {
    var redirect = request("GET", "/swagger-ui.html");
    assertTrue(redirect.statusCode() == 301 || redirect.statusCode() == 302);
    assertTrue(
        redirect.headers().firstValue("Location").orElseThrow().contains("/swagger-ui/index.html"));

    var page = request("GET", "/swagger-ui/index.html");
    assertEquals(200, page.statusCode());
  }

  private JsonNode apiDocs() throws Exception {
    return expectJson(200, request("GET", "/v3/api-docs"));
  }

  private static void assertSuccessResponses(JsonNode operation) {
    var responses = operation.path("responses");
    assertTrue(responses.has("200"));
    assertEquals(1, responses.size());
  }

  private static Set<String> requiredFields(JsonNode schema) {
    Set<String> fields = new HashSet<>();
    schema.path("required").forEach(field -> fields.add(field.asString()));
    return fields;
  }
}
