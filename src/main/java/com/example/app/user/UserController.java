package com.example.app.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @Operation(summary = "查询用户列表", description = "按 ID 升序返回")
  @GetMapping
  public List<UserResponse> list() {
    return service.list();
  }

  @Operation(summary = "查询用户详情")
  @GetMapping("/{id}")
  public UserResponse get(@Parameter(description = "用户 ID", example = "1") @PathVariable long id) {
    return service.get(id);
  }

  @Operation(summary = "新增用户")
  @PostMapping
  public UserResponse create(@Valid @RequestBody UserRequest request) {
    return service.create(request);
  }

  @Operation(summary = "修改用户", description = "省略邮箱或年龄，或将其设为 null，会清空对应字段")
  @PutMapping("/{id}")
  public UserResponse update(
      @Parameter(description = "用户 ID", example = "1") @PathVariable long id,
      @Valid @RequestBody UserRequest request) {
    return service.update(id, request);
  }

  @Operation(summary = "删除用户")
  @DeleteMapping("/{id}")
  public boolean delete(@Parameter(description = "用户 ID", example = "1") @PathVariable long id) {
    service.delete(id);
    return true;
  }
}
