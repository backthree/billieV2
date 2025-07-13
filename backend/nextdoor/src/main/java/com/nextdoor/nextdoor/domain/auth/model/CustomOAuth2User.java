package com.nextdoor.nextdoor.domain.auth.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

	private String id;

	@Getter
	private String uuid;
	private Collection<? extends GrantedAuthority> authorities;
	private Map<String, Object> attributes;

	public CustomOAuth2User(String id, String uuid, Map<String, Object> attributes) {
		this.id = id;
		this.attributes = attributes;
		this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public String getName() {
		return this.id;
	}
}
