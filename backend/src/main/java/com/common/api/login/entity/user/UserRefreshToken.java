package com.common.api.login.entity.user;

import com.common.api.login.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// TODO 로그인 추적(ip, 이벤트 타입 등) 로그를 따로 생성할 것
@Entity
@Table(name = "user_refresh_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_device",
                columnNames = {"user_id", "device_id"}
        ),
        indexes = {
                @Index(name = "idx_urt_user_device_active",
                        columnList = "user_id, device_id, revoked, refresh_token_hash")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRefreshToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", length = 500, nullable = false)
    private String refreshTokenHash;

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
                            String refreshTokenHash,
                            String deviceId,
                            String userAgent,
                            LocalDateTime expiresAt) {
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.revoked = true;
    }
}
