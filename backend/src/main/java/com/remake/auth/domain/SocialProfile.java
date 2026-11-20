package com.remake.auth.domain;

import com.remake.user.domain.AuthProvider;

/** 소셜 제공자가 돌려준 최소 프로필. 이메일은 없을 수 있다. */
public record SocialProfile(AuthProvider provider, String providerUserId, String email) {
}
