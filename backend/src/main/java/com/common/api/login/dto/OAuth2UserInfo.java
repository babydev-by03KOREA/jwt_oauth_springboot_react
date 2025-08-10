package com.common.api.login.dto;

import com.common.api.login.enums.OAuthProvider;

import java.util.Map;
import java.util.Optional;

public interface OAuth2UserInfo {
    String getId();                 // provider user id
    String getEmail();              // null 가능
    String getDisplayName();        // 닉네임/이름 등
    String getProfileImageUrl();    // null 가능
    Map<String, Object> getRawAttributes();
    OAuthProvider getProvider();
}
