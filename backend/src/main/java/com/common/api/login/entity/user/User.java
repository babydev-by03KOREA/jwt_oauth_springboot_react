package com.common.api.login.entity.user;

import com.common.api.login.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {})
public class User extends BaseEntity {
    @Column(unique = true)
    private String userId;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @Builder
    public User(String userId, String email, String password, String displayName, String profileImageUrl) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void addRole(RoleEntity role) {
        UserRole mapping = UserRole.builder()
                .user(this)
                .role(role)
                .build();
        this.userRoles.add(mapping);
    }

    public void removeRole(RoleEntity role) {
        this.userRoles.removeIf(ur -> ur.getRole().equals(role));
    }
}
