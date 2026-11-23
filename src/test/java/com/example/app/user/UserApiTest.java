package com.example.app.user;

import static org.junit.jupiter.api.Assertions.*;

import com.example.app.support.HttpTestSupport;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import tools.jackson.databind.JsonNode;

@Execution(ExecutionMode.SAME_THREAD)
class UserApiTest extends HttpTestSupport {
  private static final String USERS = "/api/users";

  private static final String ALICE_WITH_DETAILS =
      "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"age\":20}";

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void resetUserTable() {
    String database = jdbc.queryForObject("SELECT current_database()", String.class);
    assertTrue(
        "test".equals(database)
            || (database != null && database.startsWith("spring_boot_template_test")),
        "Only disposable databases named test or spring_boot_template_test* are permitted");
    jdbc.execute("DROP TABLE IF EXISTS public.test_user");
    new ResourceDatabasePopulator(new FileSystemResource("db/test_user.sql"))
        .execute(Objects.requireNonNull(jdbc.getDataSource()));
  }

  /**
   * 验证创建用户时返回提交的字段、生成的 ID 和有效时间戳，且查询结果与创建响应一致。
   */
  @Test
  void createsUserAndReturnsPersistedValues() throws Exception {
    var created = expectJson(200, request("POST", USERS, ALICE_WITH_DETAILS));

    assertTrue(created.path("id").asLong() > 0);
    assertEquals("alice", created.path("username").asString());
    assertEquals("alice@example.com", created.path("email").asString());
    assertEquals(20, created.path("age").asInt());
    OffsetDateTime.parse(created.path("createdAt").asString());
    OffsetDateTime.parse(created.path("updatedAt").asString());
    assertEquals(created, getUser(created.path("id").asLong()));
  }

  /**
   * 用用户名逆序创建的两条记录验证列表按 ID 升序返回，且未填写的可选字段为 null。
   */
  @Test
  void listsUsersByAscendingId() throws Exception {
    long first = createUser("{\"username\":\"zoe\"}");
    long second = createUser("{\"username\":\"alice\",\"email\":null}");

    var users = expectJson(200, request("GET", USERS));
    assertTrue(users.isArray());
    assertEquals(2, users.size());
    assertEquals(first, users.get(0).path("id").asLong());
    assertEquals(second, users.get(1).path("id").asLong());
    for (var user : users) {
      assertTrue(user.path("email").isNull());
      assertTrue(user.path("age").isNull());
    }
  }

  /**
   * 验证 PUT 在省略可选字段或传 null 时清空字段，且重复提交相同内容时保留首次更新后的数据和时间戳。
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"username\":\"alice2\"}",
        "{\"username\":\"alice2\",\"email\":null,\"age\":null}"
      })
  void putClearsOptionalFieldsAndLeavesRepeatedUpdatesUnchanged(String replacement)
      throws Exception {
    long id = createUser(ALICE_WITH_DETAILS);
    var before = getUser(id);
    var updated = expectJson(200, request("PUT", userPath(id), replacement));

    assertEquals("alice2", updated.path("username").asString());
    assertTrue(updated.path("email").isNull());
    assertTrue(updated.path("age").isNull());
    assertEquals(before.path("createdAt"), updated.path("createdAt"));
    assertTrue(
        OffsetDateTime.parse(updated.path("updatedAt").asString())
            .isAfter(OffsetDateTime.parse(before.path("updatedAt").asString())));
    assertEquals(updated, expectJson(200, request("PUT", userPath(id), replacement)));
    assertEquals(updated, getUser(id));
  }

  /**
   * 验证删除接口返回 true，且随后查询、更新或再次删除同一用户均返回 400 和「用户不存在」。
   */
  @Test
  void deletesUserAndReportsMissingRecord() throws Exception {
    long id = createUser("{\"username\":\"alice\"}");
    String path = userPath(id);

    var deleted = expectJson(200, request("DELETE", path));
    assertTrue(deleted.isBoolean());
    assertTrue(deleted.asBoolean());
    assertError(400, "用户不存在", request("GET", path));
    assertError(400, "用户不存在", request("PUT", path, "{\"username\":\"bob\"}"));
    assertError(400, "用户不存在", request("DELETE", path));
  }

  /**
   * 验证用户可保留自己的用户名，创建或更新时使用他人的用户名会返回 400，且失败的更新保留原值。
   */
  @Test
  void rejectsDuplicateUsernamesOnCreateAndUpdate() throws Exception {
    String alice = "{\"username\":\"alice\"}";
    long aliceId = createUser(alice);
    long bobId = createUser("{\"username\":\"bob\"}");

    expectJson(200, request("PUT", userPath(aliceId), alice));
    assertError(400, "用户名已存在", request("POST", USERS, alice));
    assertError(400, "用户名已存在", request("PUT", userPath(bobId), alice));
    assertEquals("bob", getUser(bobId).path("username").asString());
  }

  /**
   * 验证创建和更新用户时都允许使用其他用户已登记的邮箱。
   */
  @Test
  void allowsUsersToShareAnEmailAddress() throws Exception {
    createUser("{\"username\":\"alice\",\"email\":\"shared@example.com\"}");
    long bobId = createUser("{\"username\":\"bob\",\"email\":\"shared@example.com\"}");
    long carolId = createUser("{\"username\":\"carol\"}");

    assertEquals("shared@example.com", getUser(bobId).path("email").asString());
    var updated =
        expectJson(
            200,
            request(
                "PUT",
                userPath(carolId),
                "{\"username\":\"carol\",\"email\":\"shared@example.com\"}"));
    assertEquals("shared@example.com", updated.path("email").asString());
  }

  /**
   * 验证创建和更新采用相同的字段校验，且更新时先校验请求再检查用户是否存在。
   */
  @ParameterizedTest
  @MethodSource("invalidUserRequests")
  void validatesCreateAndUpdateRequests(String body, String message) throws Exception {
    assertError(400, message, request("POST", USERS, body));
    assertError(400, message, request("PUT", userPath(0), body));
  }

  static Stream<Arguments> invalidUserRequests() {
    return Stream.of(
        Arguments.of("{}", "用户名不能为空"),
        Arguments.of("{\"username\":\" \"}", "用户名不能为空"),
        Arguments.of("{\"username\":\"" + "a".repeat(51) + "\"}", "用户名长度不能超过 50 个字符"),
        Arguments.of("{\"username\":\"alice\",\"age\":-1}", "年龄不能为负数"),
        Arguments.of("{\"username\":\"alice\",\"email\":\"invalid\"}", "邮箱格式不正确"),
        Arguments.of("{\"username\":\"alice\",\"email\":\"\"}", "邮箱格式不正确"));
  }

  /**
   * 验证用户名缺失且邮箱格式错误时，400 响应只返回其中任一字段的错误消息。
   */
  @Test
  void returnsOneFieldMessageForMultipleValidationErrors() throws Exception {
    var error = expectJson(400, request("POST", USERS, "{\"email\":\"invalid\"}"));
    String message = error.path("message").asString();
    assertTrue(message.equals("用户名不能为空") || message.equals("邮箱格式不正确"), message);
    assertEquals(400, error.path("status").asInt());
    assertEquals(2, error.size());
  }

  /**
   * 验证 JSON 语法错误、请求体字段类型错误和路径参数类型错误均返回相同的 400 响应。
   */
  @Test
  void reportsMalformedJsonAndInvalidParameterTypes() throws Exception {
    String message = "请求格式或参数类型错误";
    assertError(400, message, request("POST", USERS, "{"));
    assertError(400, message, request("POST", USERS, "{\"username\":\"alice\",\"age\":\"bad\"}"));
    assertError(400, message, request("GET", USERS + "/invalid"));
  }

  /**
   * 验证访问未注册的用户接口路径时返回 404 和「请求路径不存在」。
   */
  @Test
  void returnsNotFoundForAnUnmappedApiPath() throws Exception {
    assertError(404, "请求路径不存在", request("GET", USERS + "/unmapped/path"));
  }

  /**
   * 用临时添加的年龄唯一约束触发数据库异常，验证接口返回 400 和「数据不符合约束」。
   */
  @Test
  void reportsDatabaseConstraintViolations() throws Exception {
    jdbc.execute("ALTER TABLE public.test_user ADD UNIQUE (age)");
    createUser("{\"username\":\"alice\",\"age\":20}");

    assertError(400, "数据不符合约束", request("POST", USERS, "{\"username\":\"bob\",\"age\":20}"));
  }

  /**
   * 通过重命名用户名列使查询失败，验证接口只返回通用的 500 错误消息。
   */
  @Test
  void hidesUnexpectedDatabaseErrorDetails() throws Exception {
    jdbc.execute("ALTER TABLE public.test_user RENAME COLUMN username TO unavailable_username");

    assertError(500, "服务器内部错误", request("GET", USERS));
  }

  private static String userPath(long id) {
    return USERS + "/" + id;
  }

  private long createUser(String body) throws Exception {
    return expectJson(200, request("POST", USERS, body)).path("id").asLong();
  }

  private JsonNode getUser(long id) throws Exception {
    return expectJson(200, request("GET", userPath(id)));
  }

  private void assertError(int status, String message, HttpResponse<String> response) {
    var error = expectJson(status, response);
    assertEquals(status, error.path("status").asInt());
    assertEquals(message, error.path("message").asString());
    assertEquals(2, error.size());
  }
}
