package com.common.api.login.entity.user;

import com.common.api.login.entity.BaseEntity;
import com.common.api.login.enums.RoleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 어떤 종류의 권한(역할)이 있는지 정의
 * */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleEntity extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", unique = true, nullable = false)
    private RoleType roleName;

    @Column(name = "description")
    private String description;

    @Builder
    public RoleEntity(RoleType roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}
