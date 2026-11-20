package com.remake.ingredient.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.ingredient.application.port.in.GetExpiryAlertIngredientsUseCase;
import com.remake.ingredient.application.port.out.LoadExpiryAlertIngredientPort;
import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;
import com.remake.ingredient.domain.ExpiryPolicy;

import lombok.RequiredArgsConstructor;

/** 알림용 재료 조회. 오늘 날짜는 주입받은 Clock에서만 읽고, 기준 날짜 계산은 ExpiryPolicy에 맡긴다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpiryAlertIngredientService implements GetExpiryAlertIngredientsUseCase {

	private final LoadExpiryAlertIngredientPort loadExpiryAlertIngredientPort;
	private final Clock clock;

	@Override
	public List<ExpiringIngredient> getExpiringIngredients() {
		LocalDate today = LocalDate.now(clock);
		return loadExpiryAlertIngredientPort.loadExpiringBetween(
				ExpiryPolicy.expiringFrom(today), ExpiryPolicy.expiringTo(today));
	}

	@Override
	public List<ExpiredIngredientCount> getExpiredIngredientCounts() {
		LocalDate today = LocalDate.now(clock);
		return loadExpiryAlertIngredientPort.countExpiredBefore(ExpiryPolicy.expiredBefore(today));
	}

	@Override
	public List<ExpiredIngredient> getIngredientsExpiredYesterday() {
		LocalDate today = LocalDate.now(clock);
		return loadExpiryAlertIngredientPort.loadExpiredOn(ExpiryPolicy.expiredYesterday(today));
	}
}
