package com.remake.user.domain;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 회원. 탈퇴 시 soft-delete(deleted_at)와 provider_user_id·email 비식별화는 #68에서 한다.
 */
public record User(
		Long id,
		AuthProvider provider,
		String providerUserId,
		String email,
		UserRole role,
		UserStatus status,
		UserPlan plan,
		Integer householdSize,
		boolean onboardingCompleted,
		boolean recipeSupplementEnabled,
		boolean notificationEnabled,
		String lastMealPurpose,
		LocalDateTime deletedAt) {

	public static User signUp(AuthProvider provider, String providerUserId, String email) {
		return new User(null, provider, providerUserId, email, UserRole.USER, UserStatus.ACTIVE, UserPlan.BASIC,
				null, false, false, true, null, null);
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE && deletedAt == null;
	}

	public boolean isSuspended() {
		return status == UserStatus.SUSPENDED;
	}

	public User withId(Long id) {
		return new User(id, provider, providerUserId, email, role, status, plan, householdSize, onboardingCompleted,
				recipeSupplementEnabled, notificationEnabled, lastMealPurpose, deletedAt);
	}
}
