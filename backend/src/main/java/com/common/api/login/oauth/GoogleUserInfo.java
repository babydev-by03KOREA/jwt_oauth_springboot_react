package com.common.api.login.oauth;

import com.common.api.login.dto.OAuth2UserInfo;
import com.common.api.login.enums.OAuthProvider;

import java.util.Collections;
import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attrs;

    public static GoogleUserInfo from(Map<String, Object> attributes) {
        return new GoogleUserInfo(attributes);
    }

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attrs = attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public String getId() {
        // 구글은 'sub'가 고유 사용자 id
        Object sub = attrs.get("sub");
        return sub != null ? sub.toString() : null;
    }

    @Override
    public String getEmail() {
        return (String) attrs.get("email"); // scope: email 필요
    }

    @Override
    public String getDisplayName() {
        // name (또는 given_name + family_name)
        String name = (String) attrs.get("name");
        if (name != null) return name;

        String given = (String) attrs.get("given_name");
        String family = (String) attrs.get("family_name");
        if (given != null || family != null) {
            return ((given != null) ? given : "") + ((family != null) ? " " + family : "");
        }
        return null;
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attrs.get("picture"); // scope: profile 필요
    }

    @Override
    public Map<String, Object> getRawAttributes() {
        return attrs;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }
}
