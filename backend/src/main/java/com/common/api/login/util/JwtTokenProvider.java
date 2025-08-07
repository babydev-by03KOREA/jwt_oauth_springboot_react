package com.common.api.login.util;

import com.common.api.login.entity.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;

@Component
public class JwtTokenProvider {

    private final long accessTokenValidityInMs;

    @Getter
    private final long refreshTokenValidityInMs;

    private final SecretKey secretKey;

    public JwtTokenProvider(
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityInMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityInMs,
            @Value("${jwt.secret}") String secret
    ) {
        this.accessTokenValidityInMs = accessTokenValidityInMs;
        this.refreshTokenValidityInMs = refreshTokenValidityInMs;
//        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
        try {
            // 표준 Base64 디코딩 예시
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.secretKey  = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT 비밀키 디코딩에 실패했습니다. " +
                            "application.yml의 jwt.secret 값을 확인하세요.", e
            );
        }
    }

    /**
     * AccessToken 생성 (subject: userId, claims: roles)
     */
    public String createAccessToken(String userId, Set<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityInMs);

        return Jwts.builder()
                .claim(Claims.SUBJECT, userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }


    /**
     * RefreshToken 생성 (subject: userId)
     */
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityInMs);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 로그 기록 등 예외 처리
            return false;
        }
    }

    /**
     * 토큰에서 userId(subject) 추출
     */
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰 만료 시간 확인
     */
    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }


    /**
     * 토큰 유효성 검증 및 클레임 추출
     *
     * @param token 검증할 JWT 토큰
     * @return 유효한 경우 Claims 객체 반환
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
