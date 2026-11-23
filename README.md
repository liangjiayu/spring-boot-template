# 用户 CRUD 示例

## 技术栈

| 用途 | 技术 | 说明 |
| --- | --- | --- |
| 后端 | Java 21、Spring Boot 4.1.2、Spring MVC | 提供 REST 接口 |
| 数据访问 | Spring Data JPA、Spring JDBC | JPA 实现用户 CRUD，JDBC 用于准备集成测试数据 |
| 数据库 | PostgreSQL | 集成测试使用 PostgreSQL 17 |
| 接口文档 | springdoc-openapi 3.2.0、Swagger UI | 生成 OpenAPI 规范并提供接口调试 |
| API 客户端 | TypeScript、`@umijs/openapi` | 从 OpenAPI 规范生成请求代码 |
| 构建与格式化 | Maven Wrapper、Spotless 3.1.0、google-java-format 1.28.0 | 构建项目并统一 Java 代码风格 |
| 集成测试 | Spring Boot Test、Testcontainers | 通过真实 HTTP 请求验证接口 |

## 本地启动与热更新

准备 JDK 21 和 PostgreSQL。首次启动前，创建 `crud_db` 并初始化用户表：

```sh
createdb -h localhost -U postgres crud_db
psql -h localhost -U postgres -d crud_db -f db/test_user.sql
```

应用默认连接 `jdbc:postgresql://localhost:5432/crud_db`，用户名为 `postgres`，密码为空。需要覆盖时，在启动应用的环境中设置 `DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD`。VS Code 启动调试时会继承编辑器进程的环境变量。

### VS Code

安装 Extension Pack for Java，用 VS Code 打开仓库根目录。等待 Maven 项目导入后，在「运行和调试」中选择 **Spring Boot 本地调试**，按 F5 启动。保存 Java 文件后，VS Code 会自动编译，Spring Boot DevTools 会在同一个 JVM 内重启应用上下文，应用的内存状态会随之重建。

### 命令行

在仓库根目录运行 `./mvnw spring-boot:run`。修改源码或应用配置后，在另一个终端执行 `./mvnw compile`，更新编译输出并触发 DevTools 重启。

应用启动后可访问 `http://localhost:8080/swagger-ui.html` 调试接口。
