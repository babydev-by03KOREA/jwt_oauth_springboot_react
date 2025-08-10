package com.common.api.login.security;

import com.common.api.login.dto.OAuth2UserInfo;
import com.common.api.login.entity.user.User;
import com.common.api.login.oauth.GoogleUserInfo;
import com.common.api.login.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserService userService; // 우리의 가입/조회 로직

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1) 기본 OIDC 유저 불러오기
        OidcUserService delegate = new OidcUserService();
        OidcUser oidc = delegate.loadUser(userRequest);

        // 2) 구글 속성 -> 도메인 DTO
        Map<String, Object> attrs = oidc.getAttributes();
        OAuth2UserInfo info = GoogleUserInfo.from(attrs); // id=sub, email, displayName 매핑

        // 3) 사용자 upsert
        User user = userService.processOAuth2User(info);

        // 4) 우리가 원하는 Principal로 반환
        //    - SuccessHandler는 authentication.getName()을 쓰므로 nameAttributeKey는 "sub"면 됩니다.
        //    - (DB userId도 info.getId() == sub 로 저장했으니 일치)
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new DefaultOidcUser(authorities, oidc.getIdToken(), oidc.getUserInfo(), "sub");
    }
}
