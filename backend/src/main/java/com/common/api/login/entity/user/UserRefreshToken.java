package com.common.api.login.entity.user;

import com.common.api.login.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_refresh_tokens",
        indexes = {
                @Index(name = "idx_urt_user_device_active",
                        columnList = "user_id, device_id, revoked")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRefreshToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token", length = 500, nullable = false)
    private String refreshToken;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Setter
    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Builder
    public UserRefreshToken(User user,
                            String refreshToken,
                            String deviceId,
                            String userAgent,
                            LocalDateTime expiresAt) {
        this.user         = user;
        this.refreshToken = refreshToken;
        this.deviceId     = deviceId;
        this.userAgent    = userAgent;
        this.expiresAt    = expiresAt;
    }

    public void updateToken(String newRefreshToken, LocalDateTime refreshExpiry) {
        this.refreshToken = newRefreshToken;
        this.expiresAt = refreshExpiry;
    }

    public void revoke() {
        this.revoked = true;
    }
}
