package com.roome.global.security.oauth.model;

import com.roome.domain.user.entity.User;
import com.roome.global.security.jwt.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, UserPrincipal {

	private final User user;
	private final Map<String, Object> attributes;
	private final Long id;

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(user.getUserRole().name()));
	}

	@Override
	public String getName() {
		return user.getEmail();
	}

	@Override
	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}
}
