package com.common.api.login.repository;

import com.common.api.login.entity.user.RoleEntity;
import com.common.api.login.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRoleName(RoleType roleName);
}
