package com.example.app.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class HttpTestSupport {
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @LocalServerPort private int port;
  @Autowired private ObjectMapper mapper;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String url = System.getenv("TEST_DB_URL");
    if (url == null) {
      var container = new PostgreSQLContainer("postgres:17-alpine");
      container.start();
      properties.add("spring.datasource.url", container::getJdbcUrl);
      properties.add("spring.datasource.username", container::getUsername);
      properties.add("spring.datasource.password", container::getPassword);
    } else {
      properties.add("spring.datasource.url", () -> url);
      properties.add(
          "spring.datasource.username",
          () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
      properties.add(
          "spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
    }
  }

  protected HttpResponse<String> request(String method, String path, String body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (body != null) {
      request.header("Content-Type", "application/json");
    }
    request.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
    return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  protected JsonNode expectJson(int status, HttpResponse<String> response) {
    assertEquals(status, response.statusCode(), response.body());
    return mapper.readTree(response.body());
  }
}
