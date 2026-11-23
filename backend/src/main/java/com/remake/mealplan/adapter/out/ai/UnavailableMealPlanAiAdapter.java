package com.remake.mealplan.adapter.out.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.GenerateMealPlanAiPort;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 임시 AI 어댑터: 식단표용 Gemini 연결 전까지 생성 요청을 바로 실패시킨다(AI_UPSTREAM_ERROR).
 * 그래서 서버에서 식단표를 만들면 FAILED + 실패 알림이 되고, 횟수는 차감되지 않으며 가짜 레시피가 DB에 쌓이지 않는다.
 * Gemini 어댑터를 만드는 PR(#79 다음 단계)에서 이 클래스를 지운다. 테스트는 IntegrationTestSupport에서 이 어댑터를 가짜로 바꾼다.
 */
@Slf4j
@Component
public class UnavailableMealPlanAiAdapter implements GenerateMealPlanAiPort {

	public UnavailableMealPlanAiAdapter() {
		// 생성자 주입할 것이 없다. MockedAdapterWiringTest가 생성자 하나만 있는지 본다.
	}

	@Override
	public List<GeneratedMealSlot> generate(MealPlanAiRequest request) {
		log.warn("식단표 AI가 아직 연결되지 않아 생성을 실패로 끝냄");
		throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
	}
}
