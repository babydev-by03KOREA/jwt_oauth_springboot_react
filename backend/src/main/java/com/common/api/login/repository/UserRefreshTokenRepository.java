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

import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    // 비관적 LOCK을 사용하여 조회 시점부터 해당 행에 FOR UPDATE 락 → 다른 트랜잭션은 대기 → 순차 처리
    // PESSIMISTIC_WRITE 락 + deviceId 조건 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      SELECT urt
        FROM UserRefreshToken urt
       WHERE urt.user = :user
         AND urt.refreshToken = :token
         AND urt.deviceId = :deviceId
      """)
    Optional<UserRefreshToken> findByUserAndTokenForUpdate(
            @Param("user")     User user,
            @Param("token")    String oldRefreshToken,
            @Param("deviceId") String deviceId
    );

    /**
     * 특정 디바이스의 활성화된 토큰만 소프트 삭제(revoked = true)
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
}
