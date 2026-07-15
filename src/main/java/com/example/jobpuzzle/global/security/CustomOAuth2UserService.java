package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 카카오 로그인 요청
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.ofKakao(userNameAttributeName, oAuth2User.getAttributes());

        // 정보갱신 & 회원가입처리
        User user = userRepository.findBySocialProviderAndSocialId(attributes.getProvider(), attributes.getSocialId())
                .map(existing -> {
                    existing.updateSocialProfile(attributes.getEmail(), attributes.getName());
                    return existing;
                })
                .orElseGet(() -> userRepository.save(attributes.toEntity()));

        return new CustomOAuth2User(user, attributes.getAttributes(), attributes.getNameAttributeKey());
    }
}
