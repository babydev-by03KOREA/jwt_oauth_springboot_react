package com.common.api.login.service;

import com.common.api.login.dto.*;
import com.common.api.login.entity.user.*;
import com.common.api.login.enums.OAuthProvider;
import com.common.api.login.enums.RoleType;
import com.common.api.login.repository.*;
import com.common.api.login.util.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.common.api.login.util.HashUtils.sha256Hex;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void registerUser(SignupRequest request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        User user = User.builder()
                .userId(request.getUserId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .displayName(request.getDisplayName())
                .build();

        RoleEntity role = roleRepository.findByRoleName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER가 존재하지 않습니다."));

        user.addRole(role);
        userRepository.save(user);
    }

    @Transactional
    public User processOAuth2User(OAuth2UserInfo info) {
        // 1) userId(=providerUserId) 로 먼저 조회
        return userRepository.findByUserId(info.getId())
                .orElseGet(() -> {
                    // 2) 없으면 새로 가입
                    User newUser = User.builder()
                            .userId(info.getId())
                            .email(info.getEmail())
                            .password(null)                       // OAuth 로 로그인하므로 비밀번호 없음
                            .displayName(info.getDisplayName())   // 닉네임 또는 이름
                            .build();
                    newUser = userRepository.save(newUser);

                    // 3) 기본 ROLE_USER 할당
                    RoleEntity userRole = roleRepository.findByRoleName(RoleType.ROLE_USER)
                            .orElseThrow(() ->
                                    new IllegalStateException("ROLE_USER가 미리 설정되어 있어야 합니다.")
                            );

                    UserRole ur = UserRole.builder()
                            .user(newUser)
                            .role(userRole)
                            .build();
                    userRoleRepository.save(ur);

                    return newUser;
                });
    }

    @Transactional
    public TokenResponse loginUser(LoginRequest request, String deviceId, String userAgent) {
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 아이디입니다."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId가 없습니다.");
        }

        // 같은 기기(deviceId)에 남아 있는 활성 토큰 soft-revoke
//        refreshTokenRepository.revokeByUserAndDeviceId(user, deviceId);

        // UserRole 대신 roleName 스트링 목록으로 변환
        Set<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName().name())
                .collect(Collectors.toSet());

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), roleNames);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // ChronoUnit.MILLIS 를 이용해 refreshTokenValidityInMs 밀리초만큼 더한 LocalDateTime 을 생성
        LocalDateTime refreshExpiry = LocalDateTime.now()
                .plus(jwtTokenProvider.getRefreshTokenValidityInMs(), ChronoUnit.MILLIS);

        // 1) 해당 기기의 행이 있으면 UPDATE로 교체 (revoked=false로 복구)
        int updated = refreshTokenRepository.issueOrReplace(
                user, deviceId, sha256Hex(refreshToken), refreshExpiry, userAgent
        );

        // 2) 없으면 새로 INSERT (unique (user_id, device_id) 위배 안 됨)
        if (updated == 0) {
            UserRefreshToken rt = UserRefreshToken.builder()
                    .user(user)
                    .refreshTokenHash(sha256Hex(refreshToken))
                    .deviceId(deviceId)
                    .userAgent(userAgent)
                    .expiresAt(refreshExpiry)
                    .build();
            refreshTokenRepository.save(rt);
        }

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refreshToken(
            String oldRefreshToken,
            String deviceId,
            String userAgent
    ) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId가 없습니다.");
        }
        if (!jwtTokenProvider.validateToken(oldRefreshToken)) {
            throw new IllegalArgumentException("만료되었거나 잘못된 리프레시 토큰입니다.");
        }

        String userId = jwtTokenProvider.getUserId(oldRefreshToken);

        // (roles이 필요하면 조회)
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Set<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName().name())
                .collect(Collectors.toSet());

        String newAccessToken  = jwtTokenProvider.createAccessToken(userId, roleNames);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        LocalDateTime newExpiry = LocalDateTime.now()
                .plus(jwtTokenProvider.getRefreshTokenValidityInMs(), ChronoUnit.MILLIS);

        // 핵심: 조회/락 없이 '조건 맞을 때만' 한 방에 회전
        int updated = refreshTokenRepository.rotateTokenIfValid(
                userId,
                deviceId,
                sha256Hex(oldRefreshToken),
                sha256Hex(newRefreshToken),
                newExpiry,
                userAgent,
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new IllegalArgumentException("저장된 리프레시 토큰이 아니거나 만료/폐기되었습니다.");
        }

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public User findByUserIdOrThrow(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    @Transactional
    public ProfileResponse findByUserId(String userId) {
        User user = userRepository.findByUserId(userId).orElseThrow(() -> new UsernameNotFoundException("해당하는 userId의 정보를 찾을 수 없습니다.: " + userId));
        return new ProfileResponse(
                user.getDisplayName(),
                user.getProfileImageUrl()
        );
    }

    /**
     * 로그아웃은 항상 성공처럼 응답(idempotent)
     * “이미 없는 토큰 / 이미 로그아웃됨” 같은 상황에서도 200 OK로 처리하는 게 일반적
     * why? 클라이언트는 쿠키 삭제만 잘 하면 되니까요(서버는 조용히 no-op).
     * */
    @Transactional
    public void logoutUser(String accessToken, String deviceId) {
        String userId;
        try {
            userId = jwtTokenProvider.getSubjectEvenIfExpired(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 AccessToken 입니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (deviceId != null && !deviceId.isBlank()) {
            // 디바이스 1대만 끊기 (idempotent 권장)
            refreshTokenRepository.revokeByUserAndDeviceId(user, deviceId);
        } else {
            // 모든 디바이스 끊기
            refreshTokenRepository.revokeAllByUser(user);
        }
    }

}
