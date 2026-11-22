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
