package com.common.api.login.dto;

import lombok.*;

import java.util.Map;
import java.util.Optional;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth2KakaoUserInfoDto {
    private String id;

    private int statusCode;                 // 상태 코드

    private String email;                   // 이메일

    private String nickname;                // 닉네임

    private Optional<String> profileImageUrl;         // 프로필 이미지 URL

    private String name;                    // [Biz] 사용자 이름

    private Optional<String> ageRange;                // [Biz] 사용자 나이 범위

    private String birthYear;                // [Biz] 사용자 출생 연도

    private String birthday;                // [Biz] 사용자 생일

    private Optional<String> gender;                  // [Biz] 사용자 성별

    /** 전체 응답 JSON을 담아둘 맵 */
    private Map<String,Object> rawAttributes;

    @Builder
    public OAuth2KakaoUserInfoDto(String id, int statusCode, String email, String nickname, Optional<String> profileImageUrl, String name, Optional<String> ageRange, String birthYear, String birthday, Optional<String> gender, Map<String,Object> rawAttributes) {
        this.id = id;
        this.statusCode = statusCode;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.name = name;
        this.ageRange = ageRange;
        this.birthYear = birthYear;
        this.birthday = birthday;
        this.gender = gender;
        this.rawAttributes = rawAttributes;
    }
}
