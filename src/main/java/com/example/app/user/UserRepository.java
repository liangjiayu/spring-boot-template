package com.example.app.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
  boolean existsByUsername(String username);

  boolean existsByUsernameAndIdNot(String username, long id);
}
