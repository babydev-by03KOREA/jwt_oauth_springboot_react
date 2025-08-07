package com.common.api.login.repository;

import com.common.api.login.entity.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    @EntityGraph(
            attributePaths = { "userRoles", "userRoles.role" }
    )
    Optional<User> findByUserId(String userId);
}
