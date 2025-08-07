package com.common.api.login.service;

import com.common.api.login.dto.*;
import com.common.api.login.entity.user.OAuthUser;
import com.common.api.login.entity.user.RoleEntity;
import com.common.api.login.entity.user.User;
import com.common.api.login.entity.user.UserRefreshToken;
import com.common.api.login.enums.OAuthProvider;
import com.common.api.login.enums.RoleType;
import com.common.api.login.repository.OAuthUserRepository;
import com.common.api.login.repository.RoleRepository;
import com.common.api.login.repository.UserRefreshTokenRepository;
import com.common.api.login.repository.UserRepository;
import com.common.api.login.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthUserRepository oauthUserRepository;
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
    public User processOAuth2User(OAuth2KakaoUserInfoDto dto) {
        // 1) 먼저 User 테이블에 가입 or 조회
        User user = userRepository.findByUserId(dto.getId())
                .orElseGet(() -> {
                    // 가입 로직: userId는 카카오ID, password null, displayName dto.getNickname() 등
                    User newUser = User.builder()
                            .userId(dto.getId())
                            .email(dto.getEmail())
                            .password(null)
                            .displayName(dto.getNickname())
                            .build();
                    return userRepository.save(newUser);
                });

        // 2) OAuthUser (oauth_users) 테이블에 저장/업데이트
        OAuthUser oauthUser = oauthUserRepository
                .findByProviderAndProviderUserId(OAuthProvider.KAKAO, dto.getId())
                .map(existing -> {
                    // 있으면 업데이트
                    existing.updateProfile(
                            dto.getEmail(),
                            dto.getNickname(),
                            dto.getProfileImageUrl().orElse(null),
                            dto.getRawAttributes()    // attrs 맵 전체
                    );
                    return existing;
                })
                .orElseGet(() -> {
                    // 없으면 새로 빌드
                    return oauthUserRepository.save(
                            OAuthUser.builder()
                                    .user(user)
                                    .provider(OAuthProvider.KAKAO)
                                    .providerUserId(dto.getId())
                                    .email(dto.getEmail())
                                    .displayName(dto.getNickname())
                                    .profileImageUrl(dto.getProfileImageUrl().orElse(null))
                                    .rawAttributes(dto.getRawAttributes())
                                    .build()
                    );
                });

        // 3) OAuthUser 저장 후에도 User 쪽 연관관계 유지
        oauthUserRepository.save(oauthUser);

        return user;
    }

    @Transactional
    public TokenResponse loginUser(LoginRequest request, String deviceId, String userAgent) {
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 아이디입니다."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        // 같은 기기(deviceId)에 남아 있는 활성 토큰 soft-revoke
        refreshTokenRepository.revokeByUserAndDeviceId(user, deviceId);

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

        // 새 UserRefreshToken 엔티티 생성 & 저장
        UserRefreshToken rt = UserRefreshToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .expiresAt(refreshExpiry)
                .build();
        refreshTokenRepository.save(rt);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refreshToken(
            String oldRefreshToken,
            String deviceId,
            String userAgent
    ) {
        if (!jwtTokenProvider.validateToken(oldRefreshToken)) {
            throw new IllegalArgumentException("만료되었거나 잘못된 리프레시 토큰입니다.");
        }
        String userId = jwtTokenProvider.getUserId(oldRefreshToken);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // PESSIMISTIC_WRITE 락 걸고 조회
        UserRefreshToken saved = refreshTokenRepository
                .findByUserAndTokenForUpdate(user, oldRefreshToken, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 리프레시 토큰이 아닙니다."));

        // DB 만료(expiry) 체크
        if (saved.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("서버에 저장된 리프레시 토큰이 만료되었습니다.");
        }

        // UserRole 대신 roleName 스트링 목록으로 변환
        Set<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName().name())
                .collect(Collectors.toSet());

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, roleNames);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        // ChronoUnit.MILLIS 를 이용해 refreshTokenValidityInMs 밀리초만큼 더한 LocalDateTime 을 생성
        LocalDateTime refreshExpiry = LocalDateTime.now()
                .plus(jwtTokenProvider.getRefreshTokenValidityInMs(), ChronoUnit.MILLIS);

        // DB 업데이트
        saved.updateToken(newRefreshToken, refreshExpiry);
        saved.setUserAgent(userAgent);
        refreshTokenRepository.save(saved);

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

    @Transactional
    public void logoutUser(String accessToken, String deviceId) {
        // AccessToken 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 AccessToken 입니다.");
        }

        // 토큰에서 userId 추출
        String userId = jwtTokenProvider.getUserId(accessToken);

        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // deviceId 가 들어왔으면 해당 디바이스만
        // 없으면 전체 세션을 soft-revoke
        if (deviceId != null && !deviceId.isBlank()) {
            // 특정 디바이스만 soft delete
            int updated = refreshTokenRepository.revokeByUserAndDeviceId(user, deviceId);
            if (updated == 0) {
                throw new IllegalArgumentException("해당 디바이스의 토큰이 존재하지 않거나 이미 회수되었습니다.");
            }
        } else {
            // 전체 디바이스 세션 soft delete
            refreshTokenRepository.revokeAllByUser(user);
        }
    }
}
