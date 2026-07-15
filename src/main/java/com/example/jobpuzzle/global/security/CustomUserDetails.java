package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    // 실제 회원 정보(엔티티)
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    // 암호화된 비밀번호
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 유저이름
    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    // 계정 만료 여부
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠금 여부
    @Override
    public boolean isAccountNonLocked() {
        return user.getIsLocked() == null || !user.getIsLocked();
    }

    // 비밀번호 만료 여부
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정 활성화 여부 (status가 String -> UserStatus enum으로 바뀌어서 비교 방식 수정)
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
