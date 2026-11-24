package com.remake.user.adapter.out.persistence;

import java.io.Serializable;

import com.remake.user.domain.AllergenCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * user_excluded_ingredients. 조인 테이블이라 BaseCreatedEntity를 쓰지 않는다.
 */
@Entity
@Table(name = "user_excluded_ingredients")
@IdClass(UserExcludedIngredientJpaEntity.Pk.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UserExcludedIngredientJpaEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "allergen_code", nullable = false, length = 30)
	private AllergenCode allergenCode;

	static UserExcludedIngredientJpaEntity of(Long userId, AllergenCode allergenCode) {
		UserExcludedIngredientJpaEntity entity = new UserExcludedIngredientJpaEntity();
		entity.userId = userId;
		entity.allergenCode = allergenCode;
		return entity;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	static class Pk implements Serializable {
		private Long userId;
		private AllergenCode allergenCode;
	}
}
