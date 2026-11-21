package com.remake.global.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.remake.user.domain.UserRole;

/** SecurityContext에 넣는 로그인 사용자. username은 내부 user id 문자열이다. */
public record LoginUserPrincipal(Long userId, UserRole role) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return String.valueOf(userId);
	}
}
