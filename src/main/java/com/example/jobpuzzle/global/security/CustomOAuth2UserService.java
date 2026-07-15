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

// 카카오 로그인 요청이 들어오면 스프링 시큐리티가 이 클래스를 자동으로 호출함
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 카카오 서버에 실제로 사용자 정보를 요청해서 받아옴
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 카카오가 회원 식별자로 쓰는 속성명 ("id")
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.ofKakao(userNameAttributeName, oAuth2User.getAttributes());

        // 이미 가입된 카카오 회원이면 정보 갱신, 처음이면 새로 회원가입 처리
        User user = userRepository.findBySocialProviderAndSocialId(attributes.getProvider(), attributes.getSocialId())
                .map(existing -> {
                    existing.updateSocialProfile(attributes.getEmail(), attributes.getName());
                    return existing;
                })
                .orElseGet(() -> userRepository.save(attributes.toEntity()));

        return new CustomOAuth2User(user, attributes.getAttributes(), attributes.getNameAttributeKey());
    }
}
