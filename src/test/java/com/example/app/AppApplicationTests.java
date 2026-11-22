package com.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppApplicationTests {
  static PostgreSQLContainer container;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String url = System.getenv("TEST_DB_URL");
    if (url == null) {
      container = new PostgreSQLContainer("postgres:17-alpine");
      container.start();
      properties.add("spring.datasource.url", container::getJdbcUrl);
      properties.add("spring.datasource.username", container::getUsername);
      properties.add("spring.datasource.password", container::getPassword);
    } else {
      // TEST_DB_URL must point to a disposable database: this suite resets its test_user table.
      properties.add("spring.datasource.url", () -> url);
      properties.add(
          "spring.datasource.username",
          () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
      properties.add(
          "spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
    }
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  final ObjectMapper mapper = new ObjectMapper();
  final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void setup() {
    String database = jdbc.queryForObject("SELECT current_database()", String.class);
    assertTrue(
        "test".equals(database) || database.startsWith("demo_crud_test"),
        "Only disposable databases named test or demo_crud_test* are permitted");
    jdbc.execute("DROP TABLE IF EXISTS public.test_user");
    jdbc.execute(
        "CREATE TABLE public.test_user (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, username varchar(50) NOT NULL UNIQUE, email varchar(255), age integer CHECK(age >= 0), created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP)");
  }

  HttpResponse<String> call(String method, String path, String body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/users" + path))
            .header("Content-Type", "application/json")
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  long create(String json) throws Exception {
    var response = call("POST", "", json);
    assertEquals(200, response.statusCode(), response.body());
    var tree = mapper.readTree(response.body());
    assertFalse(tree.get("createdAt").asText().isBlank());
    assertFalse(tree.get("updatedAt").asText().isBlank());
    long id = tree.get("id").asLong();
    assertTrue(id > 0);
    var stored = mapper.readTree(call("GET", "/" + id, null).body());
    assertEquals(tree.get("createdAt"), stored.get("createdAt"));
    assertEquals(tree.get("updatedAt"), stored.get("updatedAt"));
    assertTrue(response.headers().firstValue("Location").isEmpty());
    return id;
  }

  @Test
  void crudAndNulls() throws Exception {
    long id = create("{\"username\":\"alice\",\"email\":\"alice@example.com\",\"age\":20}");
    assertEquals(200, call("GET", "/" + id, null).statusCode());
    long second = create("{\"username\":\"bob\",\"email\":\"  \"}");
    var list = mapper.readTree(call("GET", "", null).body());
    assertEquals(2, list.size());
    assertEquals(id, list.get(0).get("id").asLong());
    assertEquals(second, list.get(1).get("id").asLong());
    var before = mapper.readTree(call("GET", "/" + id, null).body());
    var updated = call("PUT", "/" + id, "{\"username\":\"alice2\"}");
    assertEquals(200, updated.statusCode());
    var after = mapper.readTree(updated.body());
    assertEquals("alice2", after.get("username").asText());
    assertEquals("alice@example.com", before.get("email").asText());
    assertTrue(after.path("email").isNull());
    assertTrue(after.get("age").isNull());
    assertEquals(before.get("createdAt"), after.get("createdAt"));
    assertTrue(
        java.time.OffsetDateTime.parse(after.get("updatedAt").asText())
            .isAfter(java.time.OffsetDateTime.parse(before.get("updatedAt").asText())));
    var repeated = call("PUT", "/" + id, "{\"username\":\"alice2\"}");
    assertEquals(200, repeated.statusCode(), repeated.body());
    var repeatedBody = mapper.readTree(repeated.body());
    assertEquals(after, repeatedBody);
    var stored = mapper.readTree(call("GET", "/" + id, null).body());
    assertEquals(repeatedBody, stored);
    var deletion = call("DELETE", "/" + id, null);
    assertEquals(200, deletion.statusCode());
    assertEquals("true", deletion.body());
    var missing = call("GET", "/" + id, null);
    assertEquals(400, missing.statusCode());
    var missingError = mapper.readTree(missing.body());
    assertEquals(400, missingError.path("status").asInt());
    assertEquals("用户不存在", missingError.path("message").asText());
    assertEquals(2, missingError.size());
    assertEquals(400, call("PUT", "/" + id, "{\"username\":\"gone\"}").statusCode());
    assertEquals(400, call("DELETE", "/" + id, null).statusCode());
  }

  @Test
  void validationAndConflicts() throws Exception {
    for (String body :
        new String[] {
          "{}",
          "{\"username\":\" \"}",
          "{\"username\":\"a\",\"age\":-1}",
          "{\"username\":\"a\",\"email\":\"invalid\"}",
          "{",
          "{\"username\":\"" + "a".repeat(51) + "\"}",
          "{\"username\":\"a\",\"age\":\"bad\"}"
        }) {
      assertEquals(400, call("POST", "", body).statusCode(), body);
    }
    long aliceId = create("{\"username\":\"alice\",\"email\":\"a@example.com\"}");
    assertEquals(
        200,
        call("PUT", "/" + aliceId, "{\"username\":\"alice\",\"email\":\"a@example.com\"}")
            .statusCode());
    assertDuplicate(call("POST", "", "{\"username\":\"alice\"}"));
    create("{\"username\":\"other\",\"email\":\"a@example.com\"}");
    long id = create("{\"username\":\"bob\"}");
    assertDuplicate(call("PUT", "/" + id, "{\"username\":\"alice\"}"));
    assertEquals(
        "bob", mapper.readTree(call("GET", "/" + id, null).body()).path("username").asText());
    var sharedEmail = call("PUT", "/" + id, "{\"username\":\"bob\",\"email\":\"a@example.com\"}");
    assertEquals(200, sharedEmail.statusCode(), sharedEmail.body());
    assertEquals("a@example.com", mapper.readTree(sharedEmail.body()).path("email").asText());
    assertEquals(400, call("GET", "/invalid", null).statusCode());
    var error = mapper.readTree(call("POST", "", "{}").body());
    assertEquals(400, error.path("status").asInt());
    assertEquals("用户名不能为空", error.path("message").asText());
    assertEquals(2, error.size());
    var multipleErrors = call("POST", "", "{\"email\":\"invalid\"}");
    assertEquals(400, multipleErrors.statusCode());
    var message = mapper.readTree(multipleErrors.body()).path("message").asText();
    assertTrue(message.equals("用户名不能为空") || message.equals("邮箱格式不正确"), message);
  }

  void assertDuplicate(HttpResponse<String> response) throws Exception {
    assertEquals(400, response.statusCode(), response.body());
    assertEquals("用户名已存在", mapper.readTree(response.body()).path("message").asText());
  }

  HttpResponse<String> document(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void databaseConstraintReturnsGenericError() throws Exception {
    jdbc.execute("ALTER TABLE public.test_user ADD UNIQUE (age)");
    create("{\"username\":\"alice\",\"age\":20}");
    var response = call("POST", "", "{\"username\":\"bob\",\"age\":20}");
    assertEquals(400, response.statusCode(), response.body());
    assertEquals("数据不符合约束", mapper.readTree(response.body()).path("message").asText());
  }

  @Test
  void unexpectedDatabaseError() throws Exception {
    jdbc.execute("ALTER TABLE public.test_user RENAME COLUMN username TO unavailable_username");
    var response = call("GET", "", null);
    assertEquals(500, response.statusCode());
    var error = mapper.readTree(response.body());
    assertEquals(500, error.get("status").asInt());
    assertEquals("服务器内部错误", error.get("message").asText());
  }

  @Test
  void removedProductRouteReturnsNotFound() throws Exception {
    var missing = document("/api/test-products");
    assertEquals(404, missing.statusCode());
    assertEquals("请求路径不存在", mapper.readTree(missing.body()).path("message").asText());
  }

  @Test
  void swaggerDocumentation() throws Exception {
    var response = document("/v3/api-docs");
    assertEquals(200, response.statusCode(), response.body());
    var spec = mapper.readTree(response.body());
    assertEquals("spring-boot-template", spec.at("/info/title").asText());
    assertEquals("基于 Spring Boot 的项目模板。", spec.at("/info/description").asText());
    var paths = spec.get("paths");
    assertEquals(2, paths.size());
    assertTrue(paths.at("/" + "~1api~1users").has("get"));
    assertTrue(paths.at("/~1api~1users").has("post"));
    for (String method : new String[] {"get", "put", "delete"}) {
      assertTrue(paths.at("/~1api~1users~1{id}").has(method));
    }
    for (var path : paths) {
      for (var operation : path) {
        var responses = operation.get("responses");
        assertEquals(1, responses.size());
        assertTrue(responses.has("200"));
      }
    }
    var request = spec.at("/components/schemas/UserRequest");
    assertTrue(request.get("required").toString().contains("username"));
    assertEquals(50, request.at("/properties/username/maxLength").asInt());
    assertEquals(0, request.at("/properties/age/minimum").asInt(-1));
    var user = spec.at("/components/schemas/UserResponse");
    assertTrue(user.get("properties").has("createdAt"));
    assertEquals("string", user.at("/properties/email/type").asText());
    for (var field : user.path("required")) {
      assertNotEquals("email", field.asText());
    }
    var errorProperties = spec.at("/components/schemas/ApiErrorSchema/properties");
    assertTrue(errorProperties.has("status"));
    assertTrue(errorProperties.has("message"));
    assertEquals(2, errorProperties.size());
    assertEquals(
        "boolean",
        paths.at("/~1api~1users~1{id}/delete/responses/200/content/*~1*/schema/type").asText());
    var redirect = document("/swagger-ui.html");
    assertTrue(redirect.statusCode() == 302 || redirect.statusCode() == 301);
    var page = document("/swagger-ui/index.html");
    assertEquals(200, page.statusCode());
    assertTrue(page.body().contains("swagger-ui-bundle.js"));
    assertEquals(200, document("/swagger-ui/swagger-ui-bundle.js").statusCode());
    assertEquals(200, document("/swagger-ui/swagger-ui.css").statusCode());
    assertEquals(200, document("/v3/api-docs/swagger-config").statusCode());
  }
}
