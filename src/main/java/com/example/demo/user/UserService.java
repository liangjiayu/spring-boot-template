package com.example.demo.user;

import com.example.demo.common.ApiException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  private ApiException notFound() {
    return new ApiException("用户不存在");
  }

  public List<UserResponse> list() {
    return repository.findAll(Sort.by("id")).stream().map(UserEntity::toResponse).toList();
  }

  public UserResponse get(long id) {
    return repository.findById(id).map(UserEntity::toResponse).orElseThrow(this::notFound);
  }

  public UserResponse create(UserRequest request) {
    ensureUsernameAvailable(request.username(), null);
    return repository.saveAndFlush(new UserEntity(request)).toResponse();
  }

  @Transactional
  public UserResponse update(long id, UserRequest request) {
    var user = repository.findById(id).orElseThrow(this::notFound);
    ensureUsernameAvailable(request.username(), id);
    user.updateDetails(request);
    repository.flush();
    return user.toResponse();
  }

  private void ensureUsernameAvailable(String username, Long currentUserId) {
    boolean duplicate =
        repository.exists(
            (root, builder) -> {
              var match = builder.equal(root.get("username"), username);
              return currentUserId == null
                  ? match
                  : builder.and(match, builder.notEqual(root.get("id"), currentUserId));
            });
    if (duplicate) {
      throw new ApiException("用户名已存在");
    }
  }

  @Transactional
  public void delete(long id) {
    if (repository.delete(hasId(id)) == 0) throw notFound();
  }

  private static PredicateSpecification<UserEntity> hasId(long id) {
    return (root, builder) -> builder.equal(root.get("id"), id);
  }
}
