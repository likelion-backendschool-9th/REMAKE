package com.remake.mealplan.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.mealplan.application.port.out.RequestMealPlanGenerationPort;
import com.remake.mealplan.application.port.out.SaveMealPlanPort;
import com.remake.mealplan.domain.MealPlan;

import lombok.RequiredArgsConstructor;

/** 접수한 식단표를 GENERATING으로 저장하고, 커밋된 뒤 서버 작업이 시작되게 예약한다. */
@Service
@RequiredArgsConstructor
class MealPlanRequestWriter {

	private final SaveMealPlanPort saveMealPlanPort;
	private final RequestMealPlanGenerationPort requestMealPlanGenerationPort;

	@Transactional
	public MealPlan saveAndRequest(MealPlan request, int maxDays, boolean overwrite, Instant acceptedAt) {
		MealPlan saved = saveMealPlanPort.saveNew(request);
		requestMealPlanGenerationPort.request(
				new MealPlanGenerationJob(saved.id(), saved.userId(), maxDays, overwrite, acceptedAt));
		return saved;
	}
}
