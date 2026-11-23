package com.example.app.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class HttpTestSupport {
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @LocalServerPort private int port;
  @Autowired private ObjectMapper mapper;

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
