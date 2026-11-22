package com.example.app.user;

import com.example.app.common.ApiException;
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
    if (repository.existsByUsername(request.username())) {
      throw new ApiException("用户名已存在");
    }
    return repository.saveAndFlush(new UserEntity(request)).toResponse();
  }

  @Transactional
  public UserResponse update(long id, UserRequest request) {
    var user = repository.findById(id).orElseThrow(this::notFound);
    if (repository.existsByUsernameAndIdNot(request.username(), id)) {
      throw new ApiException("用户名已存在");
    }
    user.updateDetails(request);
    repository.flush();
    return user.toResponse();
  }

  @Transactional
  public void delete(long id) {
    if (repository.delete(hasId(id)) == 0) throw notFound();
  }

  private static PredicateSpecification<UserEntity> hasId(long id) {
    return (root, builder) -> builder.equal(root.get("id"), id);
  }
}
