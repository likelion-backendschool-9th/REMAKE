package com.remake.mealplan.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.mealplan.application.port.out.RequestMealPlanGenerationPort;
import com.remake.mealplan.application.port.out.SaveMealPlanPort;
import com.remake.mealplan.domain.MealPlan;
import com.remake.user.application.port.in.RememberMealPurposeUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 접수한 식단표를 GENERATING으로 저장하고, 고른 식단 목적을 다음 기본값으로 기억한 뒤,
 * 커밋된 뒤 서버 작업이 시작되게 예약한다. 접수 단계에서 거절되면(400·409·429) 여기까지 오지 않아 목적도 그대로다.
 */
@Service
@RequiredArgsConstructor
class MealPlanRequestWriter {

	private final SaveMealPlanPort saveMealPlanPort;
	private final RememberMealPurposeUseCase rememberMealPurposeUseCase;
	private final RequestMealPlanGenerationPort requestMealPlanGenerationPort;

	@Transactional
	public MealPlan saveAndRequest(MealPlan request, int maxDays, boolean overwrite, Instant acceptedAt) {
		MealPlan saved = saveMealPlanPort.saveNew(request);
		// 생성이 나중에 실패해도 사용자가 고른 값이라 기억한다(가이드 05 "마지막 선택을 다음 기본값으로")
		rememberMealPurposeUseCase.rememberMealPurpose(saved.userId(), saved.purpose().name());
		requestMealPlanGenerationPort.request(
				new MealPlanGenerationJob(saved.id(), saved.userId(), maxDays, overwrite, acceptedAt));
		return saved;
	}
}
