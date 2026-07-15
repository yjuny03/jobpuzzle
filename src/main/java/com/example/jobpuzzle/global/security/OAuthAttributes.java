package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.user.entity.User;
import lombok.Getter;

import java.util.Map;

// 카카오가 응답으로 주는 사용자 정보를 우리 서비스에서 쓰기 좋은 형태로 변환하는 클래스
@Getter
public class OAuthAttributes {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String provider;
    private final String socialId;
    private final String email;
    private final String name;

    private OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
                             String provider, String socialId, String email, String name) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.provider = provider;
        this.socialId = socialId;
        this.email = email;
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return new OAuthAttributes(
                attributes,
                userNameAttributeName,
                "kakao",
                String.valueOf(attributes.get("id")),
                (String) kakaoAccount.get("email"),
                (String) profile.get("nickname")
        );
    }

    public User toEntity() {
        return User.createSocialUser(provider, socialId, email, name);
    }
}
