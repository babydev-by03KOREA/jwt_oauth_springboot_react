package com.common.api.login.controller;

import com.common.api.login.dto.LoginRequest;
import com.common.api.login.dto.ProfileResponse;
import com.common.api.login.dto.SignupRequest;
import com.common.api.login.dto.TokenResponse;
import com.common.api.login.service.UserService;
import com.common.api.login.util.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid SignupRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody @Valid LoginRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @CookieValue(value = "device_id", required = false) String cookieDeviceId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response
    ) {
        // deviceId 결정 로직 (헤더 → 쿠키 → 신규 UUID + 쿠키 세팅)
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = cookieDeviceId;
        }
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            Cookie idCookie = new Cookie("device_id", deviceId);
            idCookie.setHttpOnly(true);
            idCookie.setPath("/");
            idCookie.setMaxAge(60 * 60 * 24 * 365);
            response.addCookie(idCookie);
        }
        if (userAgent == null) {
            userAgent = "unknown";
        }

        // 서비스에서 토큰 쌍 발급
        TokenResponse tokens = userService.loginUser(request, deviceId, userAgent);

        // RefreshToken 을 HttpOnly Secure Cookie 로 세팅
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)           // JS에서 못 읽게
                .secure(false)             // HTTPS에서만 전송
                .path("/api/auth/refresh")  // 갱신(endpoint)에서만 가능
                .maxAge(jwtTokenProvider.getRefreshTokenValidityInMs())
                .sameSite("Strict")       // CSRF 위험 줄이기
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        Map<String, String> body = Collections.singletonMap("accessToken", tokens.getAccessToken());
        return ResponseEntity.ok(body);
    }


    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String oldRefreshToken,

            // 앱: X-Device-Id 헤더
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,

            // 웹: 쿠키에 저장된 device_id
            @CookieValue(value = "device_id", required = false) String cookieDeviceId,

            @RequestHeader(value = "User-Agent", required = false) String userAgent,

            HttpServletResponse response
    ) {
        if (oldRefreshToken == null) {
            throw new IllegalArgumentException("Refresh token이 없습니다.");
        }

        // deviceId 우선순위: 헤더 → 쿠키 → 서버에서 신규 발급
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = cookieDeviceId;
        }
        if (deviceId == null) {
            // 웹 첫 요청일 땐 신규 UUID 쿠키로 내려주기
            deviceId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("device_id", deviceId);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 365);
            response.addCookie(cookie);
        }

        if (userAgent == null) {
            userAgent = "unknown";
        }

        TokenResponse tokens = userService.refreshToken(
                oldRefreshToken, deviceId, userAgent
        );

        // 4. 새 RefreshToken 을 다시 HttpOnly Secure Cookie 로 세팅
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")          // 혹은 "/" 전역
                .maxAge(jwtTokenProvider.getRefreshTokenValidityInMs())
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 5. 응답 바디에는 새 AccessToken 만
        return ResponseEntity.ok(Map.of("accessToken", tokens.getAccessToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserId(token);
        ProfileResponse userProfileInfo = userService.findByUserId(userId);
        return ResponseEntity.ok(userProfileInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletResponse response
    ) {
        String accessToken = authorization.replace("Bearer ", "");
        userService.logoutUser(accessToken, deviceId);

        ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")    // refresh용 쿠키 경로와 동일하게
                .maxAge(0)     // 즉시 만료
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }
}
