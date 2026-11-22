package com.example.app.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "public", name = "test_user")
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String email;
  private Integer age;

  @Generated
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp(source = SourceType.DB)
  @Column(name = "updated_at")
  private Instant updatedAt;

  protected UserEntity() {}

  UserEntity(UserRequest request) {
    updateDetails(request);
  }

  void updateDetails(UserRequest request) {
    username = request.username();
    email = request.email();
    age = request.age();
  }

  UserResponse toResponse() {
    return new UserResponse(
        id,
        username,
        email,
        age,
        createdAt.atOffset(ZoneOffset.UTC),
        updatedAt.atOffset(ZoneOffset.UTC));
  }
}
