package com.common.api.login.repository;

import com.common.api.login.entity.user.User;
import com.common.api.login.entity.user.UserRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    /**
     *
     * */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE UserRefreshToken t
                   SET t.refreshTokenHash = :hash,
                       t.expiresAt       = :expiresAt,
                       t.userAgent       = :userAgent,
                       t.revoked         = false
                 WHERE t.user     = :user
                   AND t.deviceId = :deviceId
            """)
    int issueOrReplace(
            @Param("user") User user,
            @Param("deviceId") String deviceId,
            @Param("hash") String newHash,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("userAgent") String userAgent
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
               UPDATE UserRefreshToken t
                  SET t.refreshTokenHash = :newHash,
                      t.expiresAt    = :expiresAt,
                      t.userAgent    = :userAgent,
                      t.revoked      = false
                WHERE t.user.userId = :userId
                  AND t.deviceId    = :deviceId
                  AND t.refreshTokenHash= :oldHash
                  AND t.revoked = false
                  AND t.expiresAt > :now
            """)
    int rotateTokenIfValid(
            @Param("userId") String userId,
            @Param("deviceId") String deviceId,
            @Param("oldHash") String oldHash,
            @Param("newHash") String newHash,
            @Param("expiresAt") LocalDateTime newExpiresAt,
            @Param("userAgent") String userAgent,
            @Param("now") LocalDateTime now
    );

    /**
     * 특정 디바이스의 활성화된 토큰만 소프트 삭제(revoked = true)
     * 로그아웃/보안 시나리오
     */
    @Modifying
    @Query("""
            UPDATE UserRefreshToken t
               SET t.revoked = true
             WHERE t.user = :user
               AND t.deviceId = :deviceId
               AND t.revoked = false
            """)
    int revokeByUserAndDeviceId(
            @Param("user") User user,
            @Param("deviceId") String deviceId
    );

    /**
     * 특정 사용자의 모든 활성화된 토큰 소프트 삭제
     */
    @Modifying
    @Query("""
            UPDATE UserRefreshToken t
               SET t.revoked = true
             WHERE t.user = :user
               AND t.revoked = false
            """)
    int revokeAllByUser(@Param("user") User user);

    // 비관적 LOCK을 사용하여 조회 시점부터 해당 행에 FOR UPDATE 락 → 다른 트랜잭션은 대기 → 순차 처리
   /* @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
               SELECT t
                 FROM UserRefreshToken t
                WHERE t.user = :user
                  AND t.deviceId = :deviceId
            """)
    Optional<UserRefreshToken> findByUserAndDeviceIdForUpdate(
            @Param("user") User user,
            @Param("deviceId") String deviceId
    );*/
}
