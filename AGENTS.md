# 开发规范

- Java 改动完成后，仅对本次新增或修改的文件应用项目配置的 Spotless 和 google-java-format，再运行相关编译或测试。
- 验证功能需要启动或重启本地服务时，直接使用 `./mvnw spring-boot:run`，无需确认。
