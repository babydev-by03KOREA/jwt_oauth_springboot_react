package com.common.api.login.oauth;

import com.common.api.login.dto.OAuth2UserInfo;
import com.common.api.login.enums.OAuthProvider;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings("unchecked")
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attrs;

    public static KakaoUserInfo from(Map<String, Object> attributes) {
        return new KakaoUserInfo(attributes);
    }

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attrs = attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public String getId() {
        Object id = attrs.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> acc = (Map<String, Object>) attrs.get("kakao_account");
        if (acc == null) return null;

        // 동의 플래그 체크
        boolean emailOk = Boolean.FALSE.equals(acc.get("email_needs_agreement"));
        return emailOk ? (String) acc.get("email") : null;
    }

    @Override
    public String getDisplayName() {
        Map<String, Object> acc = (Map<String, Object>) attrs.get("kakao_account");
        Map<String, Object> profile = acc != null ? (Map<String, Object>) acc.get("profile") : null;

        boolean nicknameOk = acc != null && Boolean.FALSE.equals(acc.get("profile_nickname_needs_agreement"));
        return nicknameOk && profile != null ? (String) profile.get("nickname") : null;
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> acc = (Map<String, Object>) attrs.get("kakao_account");
        Map<String, Object> profile = acc != null ? (Map<String, Object>) acc.get("profile") : null;

        // 일부 앱 설정/동의에 따라 이미지가 없을 수 있음
        boolean profileOk = acc != null && Boolean.FALSE.equals(acc.get("profile_image_needs_agreement"));
        return profileOk && profile != null ? (String) profile.get("profile_image_url") : null;
    }

    @Override
    public Map<String, Object> getRawAttributes() {
        return attrs;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }
}
