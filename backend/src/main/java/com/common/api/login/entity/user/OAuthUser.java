package com.common.api.login.entity.user;

import com.common.api.login.converter.JsonAttributeConverter;
import com.common.api.login.entity.BaseEntity;
import com.common.api.login.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Entity
@Table(name = "oauth_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider","providerUserId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthUser extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(columnDefinition = "JSON")
    @Convert(converter = JsonAttributeConverter.class)
    private Map<String, Object> rawAttributes;

    @Builder
    public OAuthUser(User user, OAuthProvider provider, String providerUserId, String email, String displayName, String profileImageUrl, Map<String, Object> rawAttributes) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.rawAttributes = rawAttributes;
    }

    public void updateProfile(String email,
                              String displayName,
                              String profileImageUrl,
                              Map<String, Object> rawAttributes) {
        this.email           = email;
        this.displayName     = displayName;
        this.profileImageUrl = profileImageUrl;
        this.rawAttributes   = rawAttributes;
    }
}
