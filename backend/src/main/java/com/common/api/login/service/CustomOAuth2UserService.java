package com.common.api.login.service;

import com.common.api.login.dto.OAuth2KakaoUserInfoDto;
import com.common.api.login.entity.user.OAuthUser;
import com.common.api.login.entity.user.User;
import com.common.api.login.enums.OAuthProvider;
import com.common.api.login.repository.OAuthUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;                   // users 테이블 CRUD
    private final OAuthUserRepository oauthUserRepository;   // oauth_users 테이블 CRUD

    // 이전에 세팅해 둔 delegate: DefaultOAuth2UserService + property_keys POST
    private final DefaultOAuth2UserService delegate;

    public CustomOAuth2UserService(UserService userService, OAuthUserRepository oauthUserRepository) {
        this.delegate = new DefaultOAuth2UserService();
        this.delegate.setRequestEntityConverter(userRequest -> {
            ClientRegistration reg = userRequest.getClientRegistration();
            String uri = reg.getProviderDetails()
                    .getUserInfoEndpoint().getUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 1) registration.getScopes() 에서 실제 property_keys 로 매핑
            List<String> propertyKeys = new ArrayList<>();
            Set<String> scopes = reg.getScopes();

            if (scopes.contains("profile_nickname")) {
                propertyKeys.add("kakao_account.profile");
            }
            if (scopes.contains("account_email")) {
                propertyKeys.add("kakao_account.email");
            }
            if (scopes.contains("gender")) {
                propertyKeys.add("kakao_account.gender");
            }
            if (scopes.contains("age_range")) {
                propertyKeys.add("kakao_account.age_range");
            }
            if (scopes.contains("birthday")) {
                propertyKeys.add("kakao_account.birthday");
            }
            // (원하는 다른 키가 있으면 여기에 추가)

            MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
            try {
                form.add("property_keys", new ObjectMapper().writeValueAsString(propertyKeys));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            return new RequestEntity<>(form, headers, HttpMethod.POST, URI.create(uri));
        });
        this.userService = userService;
        this.oauthUserRepository = oauthUserRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        // 1) 카카오도메인에서 동의된 필드만 POST로 가져온다
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        Map<String,Object> attrs = oauth2User.getAttributes();

        log.info("attrs: {}", attrs);

        // 2) DTO 빌드
        OAuth2KakaoUserInfoDto dto = buildDtoFrom(attrs);

        // 3) UserService로 가입 또는 기존회원 조회
        User user = userService.processOAuth2User(dto);

        // 4) OAuthUser(=oauth_users) 저장·갱신
        OAuthUser oauthUser = oauthUserRepository
                .findByProviderAndProviderUserId(OAuthProvider.KAKAO, dto.getId())
                .map(existing -> {
                    // 기존 레코드가 있으면, updateProfile(...)로만 상태 변경
                    existing.updateProfile(
                            dto.getEmail(),
                            dto.getNickname(),
                            dto.getProfileImageUrl().orElse(null),
                            dto.getRawAttributes()      // DTO에 rawAttributes(Map<String,Object>)를 미리 담아두세요
                    );
                    return existing;
                })
                .orElseGet(() -> {
                    // 없으면 새로 생성
                    return OAuthUser.builder()
                            .user(user)
                            .provider(OAuthProvider.KAKAO)
                            .providerUserId(dto.getId())
                            .email(dto.getEmail())
                            .displayName(dto.getNickname())
                            .profileImageUrl(dto.getProfileImageUrl().orElse(null))
                            .rawAttributes(attrs)
                            .build();
                });
        oauthUserRepository.save(oauthUser);

        // 5) Spring Security Authentication 객체에 담을 속성 준비
        Map<String,Object> principalAttrs = Map.of(
                "userId",      user.getUserId(),
                "displayName", user.getDisplayName()
        );
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 6) 최종 OAuth2User 리턴
        return new DefaultOAuth2User(authorities, principalAttrs, "userId");
    }

    private OAuth2KakaoUserInfoDto buildDtoFrom(Map<String,Object> attrs) {
        // 1) kakao_account 꺼내기
        @SuppressWarnings("unchecked")
        Map<String,Object> kakaoAccount = (Map<String,Object>) attrs.get("kakao_account");

        // 2) profile 맵이 null 이면 빈 맵으로 대체
        @SuppressWarnings("unchecked")
        Map<String,Object> profile = Optional.ofNullable(
                (Map<String,Object>) kakaoAccount.get("profile")
        ).orElseGet(Collections::emptyMap);

        // 3) 동의 플래그 확인
        boolean nicknameOk = Boolean.FALSE.equals(
                kakaoAccount.get("profile_nickname_needs_agreement")
        );
        boolean emailOk    = Boolean.FALSE.equals(kakaoAccount.get("email_needs_agreement"));
        boolean profileOk  = Boolean.FALSE.equals(kakaoAccount.get("profile_needs_agreement"));
        boolean birthdayOk = Boolean.FALSE.equals(kakaoAccount.get("birthday_needs_agreement"));
        boolean genderOk   = Boolean.FALSE.equals(kakaoAccount.get("gender_needs_agreement"));
        boolean ageOk      = Boolean.FALSE.equals(kakaoAccount.get("age_range_needs_agreement"));

        // 4) 실제 값 꺼내기 (없으면 null)
        String id       = attrs.get("id").toString();
        String nickname = nicknameOk
                ? (String) profile.get("nickname")
                : null;
        String email    = emailOk   ? (String) kakaoAccount.get("email")       : null;
        String imgUrl   = profileOk ? (String) profile.get("profile_image_url") : null;
        String birth    = birthdayOk? (String) kakaoAccount.get("birthday")     : null;
        String gender   = genderOk  ? (String) kakaoAccount.get("gender")       : null;
        String ageRange = ageOk     ? (String) kakaoAccount.get("age_range")    : null;

        // 5) DTO 빌더
        return OAuth2KakaoUserInfoDto.builder()
                .id(id)
                .statusCode(200)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(Optional.ofNullable(imgUrl))
                .name(nickname)      // 비즈니스 로직에 맞게
                .ageRange(Optional.ofNullable(ageRange))
                .birthYear("")       // 카카오 API에 birthyear scope 없으면 빈 문자열
                .birthday(birth)
                .gender(Optional.ofNullable(gender))
                .rawAttributes(attrs)
                .build();
    }

}

