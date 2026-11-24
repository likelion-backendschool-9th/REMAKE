package com.remake.user.domain;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 회원. 탈퇴 시 soft-delete(deleted_at)와 provider_user_id·email을 비운다(#68).
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
		LocalDateTime deletedAt,
		LocalDateTime createdAt) {

	/** 가구원 수 상한(1~50). 온보딩·설정 PATCH 공통. */
	public static final int MAX_HOUSEHOLD_SIZE = 50;

	public static User signUp(AuthProvider provider, String providerUserId, String email) {
		return new User(null, provider, providerUserId, email, UserRole.USER, UserStatus.ACTIVE, UserPlan.BASIC,
				null, false, false, true, null, null, null);
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE && deletedAt == null;
	}

	public boolean isSuspended() {
		return status == UserStatus.SUSPENDED;
	}

	public User withId(Long id) {
		return new User(id, provider, providerUserId, email, role, status, plan, householdSize, onboardingCompleted,
				recipeSupplementEnabled, notificationEnabled, lastMealPurpose, deletedAt, createdAt);
	}

	/**
	 * 온보딩 완료. 가구원 수는 필수(1~50). 이미 완료된 회원은 다시 호출하지 않는다.
	 */
	public User completeOnboarding(int householdSize) {
		if (onboardingCompleted) {
			throw new IllegalStateException("이미 온보딩을 마쳤습니다.");
		}
		validateHouseholdSize(householdSize);
		return new User(id, provider, providerUserId, email, role, status, plan, householdSize, true,
				recipeSupplementEnabled, notificationEnabled, lastMealPurpose, deletedAt, createdAt);
	}

	/**
	 * 설정 PATCH. null인 필드는 그대로 둔다. householdSize를 보내면 1~50이어야 한다.
	 */
	public User updateProfile(Integer householdSize, Boolean recipeSupplementEnabled, Boolean notificationEnabled) {
		Integer nextHousehold = this.householdSize;
		if (householdSize != null) {
			validateHouseholdSize(householdSize);
			nextHousehold = householdSize;
		}
		return new User(id, provider, providerUserId, email, role, status, plan, nextHousehold, onboardingCompleted,
				recipeSupplementEnabled != null ? recipeSupplementEnabled : this.recipeSupplementEnabled,
				notificationEnabled != null ? notificationEnabled : this.notificationEnabled,
				lastMealPurpose, deletedAt, createdAt);
	}

	/**
	 * 탈퇴. provider_user_id·email을 비우고 deleted_at을 남긴다. 행은 유지한다.
	 */
	public User withdraw(LocalDateTime deletedAt) {
		if (deletedAt == null) {
			throw new IllegalArgumentException("탈퇴 시각이 필요합니다.");
		}
		if (this.deletedAt != null) {
			throw new IllegalStateException("이미 탈퇴한 회원입니다.");
		}
		return new User(id, provider, null, null, role, status, plan, householdSize, onboardingCompleted,
				recipeSupplementEnabled, notificationEnabled, lastMealPurpose, deletedAt, createdAt);
	}

	/** 식단표 생성에서 고른 식단 목적을 다음 기본값으로 기억한다. */
	public User rememberMealPurpose(String mealPurpose) {
		if (mealPurpose == null || mealPurpose.isBlank()) {
			throw new IllegalArgumentException("식단 목적이 필요합니다.");
		}
		return new User(id, provider, providerUserId, email, role, status, plan, householdSize, onboardingCompleted,
				recipeSupplementEnabled, notificationEnabled, mealPurpose, deletedAt);
	}

	private static void validateHouseholdSize(int householdSize) {
		if (householdSize < 1 || householdSize > MAX_HOUSEHOLD_SIZE) {
			throw new IllegalArgumentException("가구원 수는 1명 이상 " + MAX_HOUSEHOLD_SIZE + "명 이하여야 합니다.");
		}
	}
}
