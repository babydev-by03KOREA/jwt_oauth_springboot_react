package com.common.api.login.service;

import com.common.api.login.oauth.KakaoUserInfo;
import com.common.api.login.oauth.GoogleUserInfo;
import com.common.api.login.dto.OAuth2UserInfo;
import com.common.api.login.entity.user.OAuthUser;
import com.common.api.login.entity.user.User;
import com.common.api.login.enums.OAuthProvider;
import com.common.api.login.repository.OAuthUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final OAuthUserRepository oauthUserRepository;

    // 카카오 전용 delegate (property_keys POST)
    private final DefaultOAuth2UserService kakaoDelegate = new DefaultOAuth2UserService();

    // 기본 delegate (구글 등)
    private final DefaultOAuth2UserService defaultDelegate = new DefaultOAuth2UserService();

    @PostConstruct
    void initKakaoDelegate() {
        kakaoDelegate.setRequestEntityConverter(userRequest -> {
            var reg = userRequest.getClientRegistration();
            var uri = reg.getProviderDetails().getUserInfoEndpoint().getUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 스코프 -> property_keys 매핑
            Set<String> scopes = reg.getScopes();
            List<String> keys = new ArrayList<>();
            if (scopes.contains("profile_nickname")) keys.add("kakao_account.profile");
            if (scopes.contains("account_email"))    keys.add("kakao_account.email");
            if (scopes.contains("gender"))           keys.add("kakao_account.gender");
            if (scopes.contains("age_range"))        keys.add("kakao_account.age_range");
            if (scopes.contains("birthday"))         keys.add("kakao_account.birthday");

            MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
            try {
                form.add("property_keys", new ObjectMapper().writeValueAsString(keys));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            return new RequestEntity<>(form, headers, HttpMethod.POST, URI.create(uri));
        });
    }

    @Override
    @Transactional // upsert/save 가 들어오므로 트랜잭션 권장
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        final String provider = Optional.ofNullable(req.getClientRegistration().getRegistrationId())
                .orElse("")
                .toLowerCase();

        OAuth2User raw;
        OAuth2UserInfo info;
        OAuthProvider prov;

        switch (provider) {
            case "kakao" -> {
                // 카카오는 property_keys POST 세팅된 delegate 사용
                raw  = kakaoDelegate.loadUser(req);
                info = KakaoUserInfo.from(raw.getAttributes());
                prov = OAuthProvider.KAKAO;
            }
            case "google" -> {
                // 구글은 기본 GET delegate
                raw  = defaultDelegate.loadUser(req);
                info = GoogleUserInfo.from(raw.getAttributes());
                prov = OAuthProvider.GOOGLE;
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        // 1) users 테이블 upsert/조회
        User user = userService.processOAuth2User(info);

        // 2) oauth_users 테이블 upsert
        upsertOAuthUser(user, prov, info, raw.getAttributes());

        // 3) 인증 주체 빌드 (SuccessHandler가 userId 읽어가는 구조라면 키 이름 일치!)
        return buildPrincipal(user);
    }

    private void upsertOAuthUser(
            User user, OAuthProvider provider, OAuth2UserInfo info, Map<String,Object> attrs) {

        OAuthUser entity = oauthUserRepository
                .findByProviderAndProviderUserId(provider, info.getId())
                .map(e -> { e.updateProfile(info.getEmail(), info.getDisplayName(),
                        info.getProfileImageUrl(), attrs);
                    return e; })
                .orElseGet(() -> OAuthUser.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(info.getId())
                        .email(info.getEmail())
                        .displayName(info.getDisplayName())
                        .profileImageUrl(info.getProfileImageUrl())
                        .rawAttributes(attrs)
                        .build());
        oauthUserRepository.save(entity);
    }

    private OAuth2User buildPrincipal(User user) {
        Map<String, Object> attrs = Map.of(
                "userId",      user.getUserId(),
                "displayName", user.getDisplayName()
        );
        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "userId" // getName() 용 key
        );
    }
}