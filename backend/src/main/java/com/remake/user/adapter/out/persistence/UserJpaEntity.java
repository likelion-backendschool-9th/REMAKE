package com.remake.user.adapter.out.persistence;

import java.time.LocalDateTime;

import com.remake.global.common.BaseTimeEntity;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;
import com.remake.user.domain.UserPlan;
import com.remake.user.domain.UserRole;
import com.remake.user.domain.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UserJpaEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider provider;

	@Column(name = "provider_user_id", length = 255)
	private String providerUserId;

	@Column(length = 320)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserPlan plan;

	@Column(name = "household_size")
	private Integer householdSize;

	@Column(name = "onboarding_completed", nullable = false)
	private boolean onboardingCompleted;

	@Column(name = "recipe_supplement_enabled", nullable = false)
	private boolean recipeSupplementEnabled;

	@Column(name = "notification_enabled", nullable = false)
	private boolean notificationEnabled;

	@Column(name = "last_meal_purpose", length = 50)
	private String lastMealPurpose;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	static UserJpaEntity from(User user) {
		UserJpaEntity entity = new UserJpaEntity();
		entity.id = user.id();
		entity.apply(user);
		return entity;
	}

	void apply(User user) {
		this.provider = user.provider();
		this.providerUserId = user.providerUserId();
		this.email = user.email();
		this.role = user.role();
		this.status = user.status();
		this.plan = user.plan();
		this.householdSize = user.householdSize();
		this.onboardingCompleted = user.onboardingCompleted();
		this.recipeSupplementEnabled = user.recipeSupplementEnabled();
		this.notificationEnabled = user.notificationEnabled();
		this.lastMealPurpose = user.lastMealPurpose();
		this.deletedAt = user.deletedAt();
	}

	User toDomain() {
		return new User(id, provider, providerUserId, email, role, status, plan, householdSize, onboardingCompleted,
				recipeSupplementEnabled, notificationEnabled, lastMealPurpose, deletedAt);
	}
}
