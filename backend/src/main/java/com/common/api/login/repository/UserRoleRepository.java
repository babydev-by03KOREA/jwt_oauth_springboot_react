package com.common.api.login.repository;

import com.common.api.login.entity.user.User;
import com.common.api.login.entity.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser(User user);
}
