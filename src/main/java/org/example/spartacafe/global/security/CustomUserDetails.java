package org.example.spartacafe.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * [Security] 인증된 사용자의 정보를 담는 커스텀 UserDetails 클래스
 * <p>
 * userId, loginId, role 정보를 유지하며 DB 조회 없이 JWT 클레임만으로 생성됩니다.
 * role은 "USER" 형태로 저장되며, getAuthorities() 호출 시 자동으로 "ROLE_" 접두사가 붙습니다.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long userId;
    private final String loginId;
    private final String role;

    public CustomUserDetails(Long userId, String loginId, String role) {
        this.userId = userId;
        this.loginId = loginId;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security 의 hasRole("USER") 매칭을 위해 ROLE_ 접두사 자동 부여
        String authority = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}