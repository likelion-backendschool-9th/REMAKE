package com.remake.ingredient.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.out.LoadExpiryAlertIngredientPort;
import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;
import com.remake.ingredient.domain.IngredientStatus;

import lombok.RequiredArgsConstructor;

/** 알림용 재료 읽기 Port 구현. ACTIVE 재료만 읽는다. 엔티티를 거치지 않고 필요한 값만 도메인 객체로 읽는다. */
@Component
@RequiredArgsConstructor
class ExpiryAlertPersistenceAdapter implements LoadExpiryAlertIngredientPort {

	private final IngredientJpaRepository ingredientRepository;

	@Override
	public List<ExpiringIngredient> loadExpiringBetween(LocalDate from, LocalDate to) {
		return ingredientRepository.findExpiringBetween(IngredientStatus.ACTIVE, from, to);
	}

	@Override
	public List<ExpiredIngredientCount> countExpiredBefore(LocalDate date) {
		return ingredientRepository.countByUserExpiredBefore(IngredientStatus.ACTIVE, date);
	}

	@Override
	public List<ExpiredIngredient> loadExpiredOn(LocalDate date) {
		return ingredientRepository.findExpiredOn(IngredientStatus.ACTIVE, date);
	}
}
