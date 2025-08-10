package com.common.api.login.security;

import com.common.api.login.entity.user.User;
import com.common.api.login.entity.user.UserRefreshToken;
import com.common.api.login.repository.UserRefreshTokenRepository;
import com.common.api.login.service.UserService;
import com.common.api.login.util.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.common.api.login.util.HashUtils.sha256Hex;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserRefreshTokenRepository refreshTokenRepo;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        // 1) 시큐리티 Context에서 로그인된 OAuth2User 꺼내기
        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
//        String userId = oauthUser.getAttribute("userId");
        String userId = authentication.getName();
        User user = userService.findByUserIdOrThrow(userId);

        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 3) deviceId, userAgent 결정 (OAuth2 로그인 직후라 헤더에서 꺼내기)
        String deviceId = request.getHeader("X-Device-Id");
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            Cookie idCookie = new Cookie("device_id", deviceId);
            idCookie.setHttpOnly(true);
            idCookie.setPath("/");
            idCookie.setMaxAge(60 * 60 * 24 * 365);
            response.addCookie(idCookie);
        }
        String userAgent = request.getHeader("User-Agent");

        // 4) DB에 RefreshToken 저장
        UserRefreshToken entity = UserRefreshToken.builder()
                .user(user)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .refreshTokenHash(sha256Hex(refreshToken))
                .expiresAt(LocalDateTime.now()
                        .plus(jwtTokenProvider.getRefreshTokenValidityInMs(), ChronoUnit.MILLIS))
                .build();
        refreshTokenRepo.save(entity);

        // 5) HttpOnly Cookie 에 RefreshToken 세팅
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh")
                .maxAge(jwtTokenProvider.getRefreshTokenValidityInMs() / 1000)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String targetUrl = UriComponentsBuilder
                .fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("skipRefresh", "1")
                .build().toString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
