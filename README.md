# 用户 CRUD 示例

Java 21 + Spring Boot + Spring JDBC + PostgreSQL。

## Java 格式化

项目使用 Spotless 3.1.0 + google-java-format 1.28.0，采用默认 Google 风格。
完成功能修改后，再格式化本次涉及的 Java 文件，然后运行相关编译或测试：

```sh
./mvnw spotless:apply '-DspotlessFiles=.*/src/main/java/com/example/app/Example\.java'
./mvnw spotless:check '-DspotlessFiles=.*/src/main/java/com/example/app/Example\.java'
```

将示例路径替换为实际文件，多个文件正则用逗号分隔。参数是匹配绝对路径的 Java 正则，前面的 `.*/` 和文件名中的 `\.` 需保留。
格式化会调整指定文件的整体排版。省略 `spotlessFiles` 会处理所有 Java 源码，日常修改请指定文件，避免产生无关差异。
已有 Java 源码已统一格式化，后续按需处理本次修改的文件。未绑定构建阶段或安装自动改写钩子。AI 执行约定见 [AGENTS.md](AGENTS.md)。

## 启动

现有数据库必须包含 `public.test_user` 表，首次建表执行 `db/test_user.sql`。应用不会自动创建或修改表。

用户表包含自增主键 `id`、用户名 `username`、邮箱 `email`、年龄 `age`、创建时间 `created_at`、更新时间 `updated_at`。用户名唯一，邮箱可以重复，年龄非负。创建时生成时间，用户接口更新时刷新 `updated_at`。接口路径保持 `/api/users`。

```sh
./mvnw spring-boot:run
```

默认连接 `jdbc:postgresql://localhost:5432/crud_db`，用户名 `postgres`，密码为空。
可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量覆盖。默认 HTTP 端口为 8080。

## Swagger 接口文档与调试

启动后打开 http://localhost:8080/swagger-ui.html （会跳转到 Swagger UI 页面）。
OpenAPI JSON 地址：http://localhost:8080/v3/api-docs。

展开“用户管理”，选择一个接口，点击 **Try it out**，填写参数后点击 **Execute**。
建议先 POST 新增用户，复制响应中的 `id`，再用该 ID 查询详情、修改、删除。
GET `/api/users` 可查看列表。测试时使用独立用户名，结束后删除自己新增的测试记录。

各接口文档展示成功响应，错误响应在公共说明中统一定义。Swagger 默认开启，无需登录，执行请求会实际操作当前配置的数据库。
手动调试统一使用 Swagger，不再维护 `.http` 文件；自动化验证仍使用集成测试。

用户响应包含 `id`、`username`、`email`、`age`、`createdAt` 和 `updatedAt`。邮箱和年龄无值时返回 `null`。更新用户时刷新 `updatedAt`，保留 `createdAt`。两个时间均以带偏移的 ISO 8601 字符串返回。

用户名必填且不能全为空白，最长 50 字符，唯一性由 PostgreSQL 约束保证。邮箱可空，空白邮箱转为 null，非空时必须符合邮箱格式且最长 255 字符。年龄可空，提供时必须为非负整数。

错误响应包含 `status` 和 `message`。字段校验错误额外包含 `fieldErrors`。用户接口成功返回 200。参数错误、用户不存在、用户名重复以及数据库约束冲突返回 400，未预期的服务端异常返回 500。新增返回实体，删除返回 JSON 布尔值 `true`。接口未配置登录鉴权。

已用旧版脚本建表的数据库可以执行以下 SQL，允许不同用户使用同一个邮箱。

```sql
ALTER TABLE public.test_user DROP CONSTRAINT IF EXISTS test_user_email_key;
```

## 测试

有 Docker 环境时运行：

```sh
./mvnw test
```

测试自动使用 PostgreSQL 17 容器。没有 Docker 时可使用独立的临时数据库：

```sh
createdb -h localhost -U postgres demo_crud_test
TEST_DB_URL=jdbc:postgresql://localhost:5432/demo_crud_test ./mvnw test
dropdb -h localhost -U postgres demo_crud_test
```

可设置 `TEST_DB_USERNAME` 和 `TEST_DB_PASSWORD`，默认 postgres / 空密码。测试会重建目标库的 `public.test_user` 表；仅允许数据库名为 `test` 或以 `demo_crud_test` 开头，务必使用专用测试库。

集成测试通过真实 HTTP 请求覆盖完整 CRUD、自动生成字段、可空字段、唯一冲突、无效参数及不存在的用户。
